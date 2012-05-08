/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.rest.service;

import com.google.common.base.Function;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.zanata.ApplicationConfiguration;
import org.zanata.common.EntityStatus;
import org.zanata.common.LocaleId;
import org.zanata.common.MergeType;
import org.zanata.dao.DocumentDAO;
import org.zanata.dao.ProjectDAO;
import org.zanata.dao.ProjectIterationDAO;
import org.zanata.dao.TextFlowTargetDAO;
import org.zanata.exception.ZanataServiceException;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HTextFlowTarget;
import org.zanata.rest.NoSuchEntityException;
import org.zanata.rest.ReadOnlyEntityException;
import org.zanata.rest.dto.resource.TextFlowTarget;
import org.zanata.rest.dto.resource.TranslationsResource;
import org.zanata.security.ZanataIdentity;
import org.zanata.service.CopyTransService;
import org.zanata.service.LocaleService;
import org.zanata.service.TranslationService;
import org.zanata.util.StringUtil;

import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zanata.rest.service.SourceDocResource.RESOURCE_SLUG_TEMPLATE;

@Name("translatedDocResourceService")
@Path(TranslatedDocResourceService.SERVICE_PATH)
@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Transactional
/**
 * This service allows clients to push and pull both source documents and translations.
 */
public class TranslatedDocResourceService implements TranslatedDocResource
{

   // security actions
   private static final String ACTION_IMPORT_TEMPLATE = "import-template";
   private static final String ACTION_IMPORT_TRANSLATION = "import-translation";

   public static final String SERVICE_PATH = ProjectIterationService.SERVICE_PATH + "/r";

   public static final String EVENT_COPY_TRANS = "org.zanata.rest.service.copyTrans";
   
   /** Project Identifier. */
   @PathParam("projectSlug")
   private String projectSlug;

   /** Project Iteration identifier. */
   @PathParam("iterationSlug")
   private String iterationSlug;

   /** (This parameter is optional and is currently not used) */
   @HeaderParam("Content-Type")
   @Context
   private MediaType requestContentType;

   @Context
   private HttpHeaders headers;

   @Context
   private Request request;

   @Context
   private UriInfo uri;

   @In
   private ZanataIdentity identity;

   @In
   private ApplicationConfiguration applicationConfiguration;

   @In
   private ProjectIterationDAO projectIterationDAO;

   @In
   private ProjectDAO projectDAO;

   @In
   private DocumentDAO documentDAO;

   @In
   private TextFlowTargetDAO textFlowTargetDAO;

   @In
   private ResourceUtils resourceUtils;

   @In
   private ETagUtils eTagUtils;
   
   @In
   private CopyTransService copyTransServiceImpl;

   @In
   private TranslationService translationServiceImpl;

   private final Log log = Logging.getLog(TranslatedDocResourceService.class);

   @In
   private LocaleService localeServiceImpl;


   private HLocale validateTargetLocale(LocaleId locale, String projectSlug, String iterationSlug)
   {
      HLocale hLocale;
      try
      {
         hLocale = localeServiceImpl.validateLocaleByProjectIteration(locale, projectSlug, iterationSlug);
         return hLocale;
      }
      catch (ZanataServiceException e)
      {
         throw new WebApplicationException(Response.status(Status.FORBIDDEN).entity(e.getMessage()).build());
      }
   }

   private HLocale validateSourceLocale(LocaleId locale)
   {
      try
      {
         return localeServiceImpl.validateSourceLocale(locale);
      }
      catch (ZanataServiceException e)
      {
         throw new WebApplicationException(Response.status(Status.FORBIDDEN).entity(e.getMessage()).build());
      }
   }
   

   /**
    * Retrieves a set of translations for a given locale.
    * 
    * @param idNoSlash The document identifier. Some document ids could have forward slashes ('/') in them which would
    * cause conflicts with the browser's own url interpreter. For this reason, the supplied id must have all its '/' 
    * characters replaced with commas (',').
    * @param locale The locale for which to get translations.
    * @param extensions The translation extensions to retrieve (e.g. "comment"). This parameter
    * allows multiple values.
    * @return The following response status codes will be returned from this operation:<br>
    * OK(200) - Successfully retrieved translations. The data will be contained in the response.<br>
    * NOT FOUND(404) - If a project, project iteration or document could not be found with the given parameters. Also
    *                  if no translations are found for the given document and locale.<br>
    * INTERNAL SERVER ERROR(500) - If there is an unexpected error in the server while performing this operation.
    */
   @Override
   @GET
   @Path(RESOURCE_SLUG_TEMPLATE + "/translations/{locale}")
   @TypeHint(TranslationsResource.class)
   // /r/{id}/translations/{locale}
   public Response getTranslations(
         @PathParam("id") String idNoSlash,
         @PathParam("locale") LocaleId locale,
         @QueryParam("ext") Set<String> extensions,
         @QueryParam("skeletons") @DefaultValue("false") boolean skeletons
         )
   {
      log.debug("start to get translation");
      String id = URIHelper.convertFromDocumentURIId(idNoSlash);
      HProjectIteration hProjectIteration = retrieveAndCheckIteration(false);

      resourceUtils.validateExtensions(extensions);

      // TODO create valid etag
      EntityTag etag = eTagUtils.generateETagForDocument(hProjectIteration, id, extensions);

      ResponseBuilder response = request.evaluatePreconditions(etag);
      if (response != null)
      {
         return response.build();
      }

      HDocument document = documentDAO.getByDocIdAndIteration(hProjectIteration, id);
      if (document.isObsolete())
      {
         return Response.status(Status.NOT_FOUND).build();
      }

      HLocale hLocale = validateTargetLocale(locale, projectSlug, iterationSlug);
      TranslationsResource translationResource = new TranslationsResource();
      boolean foundData = resourceUtils.transferToTranslationsResource(
            translationResource, document, hLocale, extensions, 
            textFlowTargetDAO.findTranslations(document, hLocale));

      if (!foundData && !skeletons)
      {
         return Response.status(Status.NOT_FOUND).build();
      }

      // TODO lastChanged
      return Response.ok().entity(translationResource).tag(etag).build();
   }

   /**
    * Deletes a set of translations for a given locale. Also deletes any extensions recorded for the translations in
    * question. The system will keep history of the translations.
    * 
    * @param idNoSlash The document identifier. Some document ids could have forward slashes ('/') in them which would
    * cause conflicts with the browser's own url interpreter. For this reason, the supplied id must have all its '/' 
    * characters replaced with commas (',').
    * @param locale The locale for which to get translations.
    * @return The following response status codes will be returned from this operation:<br>
    * OK(200) - Successfully deleted the translations.<br>
    * NOT FOUND(404) - If a project, project iteration or document could not be found with the given parameters.
    * UNAUTHORIZED(401) - If the user does not have the proper permissions to perform this operation.<br>
    * INTERNAL SERVER ERROR(500) - If there is an unexpected error in the server while performing this operation.
    */
   @Override
   @DELETE
   @Path(RESOURCE_SLUG_TEMPLATE + "/translations/{locale}")
   @Restrict("#{s:hasPermission(translatedDocResourceService.securedIteration.project, 'modify-translation')}")
   // /r/{id}/translations/{locale}
   public Response deleteTranslations(@PathParam("id") String idNoSlash, @PathParam("locale") LocaleId locale)
   {
      String id = URIHelper.convertFromDocumentURIId(idNoSlash);
      HProjectIteration hProjectIteration = retrieveAndCheckIteration(true);

      // TODO find correct etag
      EntityTag etag = eTagUtils.generateETagForDocument(hProjectIteration, id, new HashSet<String>());

      ResponseBuilder response = request.evaluatePreconditions(etag);
      if (response != null)
      {
         return response.build();
      }

      HDocument document = documentDAO.getByDocIdAndIteration(hProjectIteration, id);
      if (document.isObsolete())
      {
         return Response.status(Status.NOT_FOUND).build();
      }
      List<HTextFlowTarget> targets = textFlowTargetDAO.findAllTranslations(document, locale);

      for (HTextFlowTarget target : targets)
      {
         target.clear();
      }

      // we also need to delete the extensions here
      document.getPoTargetHeaders().remove(locale);
      textFlowTargetDAO.flush();

      return Response.ok().build();

   }

   /**
    * Updates the translations for a document and a locale.
    * 
    * @param idNoSlash The document identifier. Some document ids could have forward slashes ('/') in them which would
    * cause conflicts with the browser's own url interpreter. For this reason, the supplied id must have all its '/' 
    * characters replaced with commas (',').
    * @param locale The locale for which to get translations.
    * @param messageBody The translations to modify.
    * @param extensions The translation extension types to modify (e.g. "comment"). This parameter
    * allows multiple values.
    * @param merge Indicates how to deal with existing translations (valid options: 'auto', 'import'). Import will 
    * overwrite all current values with the values being pushed (even empty ones), while Auto will check the history 
    * of your translations and will not overwrite any translations for which it detects a previous value is being pushed.
    * @return The following response status codes will be returned from this operation:<br>
    * OK(200) - Translations were successfully updated.<br>
    * NOT FOUND(404) - If a project, project iteration or document could not be found with the given parameters.<br>
    * UNAUTHORIZED(401) - If the user does not have the proper permissions to perform this operation.<br>
    * BAD REQUEST(400) - If there are problems with the parameters passed. i.e. Merge type is not one of the accepted 
    * types. This response should have a content message indicating a reason.<br>
    * INTERNAL SERVER ERROR(500) - If there is an unexpected error in the server while performing this operation.
    */
   @Override
   @PUT
   @Path(RESOURCE_SLUG_TEMPLATE + "/translations/{locale}")
   // /r/{id}/translations/{locale}
   public Response putTranslations(@PathParam("id") String idNoSlash, @PathParam("locale") LocaleId locale, TranslationsResource messageBody, @QueryParam("ext") Set<String> extensions, @QueryParam("merge") @DefaultValue("auto") String merge)
   {
      // check security (cannot be on @Restrict as it refers to method parameters)
      identity.checkPermission("modify-translation", this.localeServiceImpl.getByLocaleId(locale),
            this.getSecuredIteration().getProject());

      log.debug("start put translations");
      MergeType mergeType;
      try
      {
         mergeType = MergeType.valueOf(merge.toUpperCase());
      }
      catch (Exception e)
      {
         return Response.status(Status.BAD_REQUEST).entity("bad merge type "+merge).build();
      }
      String id = URIHelper.convertFromDocumentURIId(idNoSlash);

      HProjectIteration hProjectIteration = projectIterationDAO.getBySlug(projectSlug, iterationSlug);

      // TODO create valid etag
      EntityTag etag = eTagUtils.generateETagForDocument(hProjectIteration, id, new HashSet<String>(0));

      ResponseBuilder response = request.evaluatePreconditions(etag);
      if (response != null)
      {
         return response.build();
      }

      // Translate
      Collection<TextFlowTarget> unknownResIds =
         this.translationServiceImpl.translateAllInDoc(projectSlug, iterationSlug, id, locale, messageBody, extensions, mergeType);


      // Regenerate etag in case it has changed
      // TODO create valid etag
      etag = eTagUtils.generateETagForDocument(hProjectIteration, id, new HashSet<String>(0));

      log.debug("successful put translation");
      // TODO lastChanged
      if (unknownResIds.isEmpty())
      {
         return Response.ok().tag(etag).build();
      }
      else
      {
         String resIdStr = StringUtil.concat(unknownResIds, ',',
            new Function<TextFlowTarget, String>()
            {
               @Override
               public String apply(@Nullable TextFlowTarget from)
               {
                  return from.getResId();
               }
            }
         );
         return Response.ok("warning: unknown resIds: " + unknownResIds).tag(etag).build();
      }
   }

   private HProjectIteration retrieveAndCheckIteration(boolean writeOperation)
   {
      HProjectIteration hProjectIteration = projectIterationDAO.getBySlug(projectSlug, iterationSlug);
      HProject hProject = hProjectIteration == null ? null : hProjectIteration.getProject();

      if (hProjectIteration == null)
      {
         throw new NoSuchEntityException("Project Iteration '" + projectSlug + ":" + iterationSlug + "' not found.");
      }
      else if (hProjectIteration.getStatus().equals(EntityStatus.OBSOLETE) || hProject.getStatus().equals(EntityStatus.OBSOLETE))
      {
         throw new NoSuchEntityException("Project Iteration '" + projectSlug + ":" + iterationSlug + "' not found.");
      }
      else if (writeOperation)
      {
         if (hProjectIteration.getStatus().equals(EntityStatus.READONLY) || hProject.getStatus().equals(EntityStatus.READONLY))
         {
            throw new ReadOnlyEntityException("Project Iteration '" + projectSlug + ":" + iterationSlug + "' is read-only.");
         }
         else
         {
            return hProjectIteration;
         }
      }
      else
      {
         return hProjectIteration;
      }
   }

   public void copyClosestEquivalentTranslation(HDocument document)
   {
      if (applicationConfiguration.getEnableCopyTrans())
      {
         copyTransServiceImpl.copyTransForDocument(document);
      }
   }
   
   public HProjectIteration getSecuredIteration()
   {
      return retrieveAndCheckIteration(false);
   }

}
