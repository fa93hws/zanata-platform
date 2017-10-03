package org.zanata.rest.service;

import static org.zanata.common.EntityStatus.OBSOLETE;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.EntityTag;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;

import javax.inject.Inject;
import javax.inject.Named;

import org.zanata.dao.DocumentDAO;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.model.po.HPoHeader;
import org.zanata.rest.NoSuchEntityException;
import org.zanata.rest.dto.extensions.gettext.PoHeader;
import org.zanata.util.HashUtil;

@Named("eTagUtils")
@javax.enterprise.context.Dependent
public class ETagUtils implements Serializable {

    private static final long serialVersionUID = -9048796062192670716L;
    @Inject
    private Session session;

    @Inject
    private DocumentDAO documentDAO;

    @Inject
    private RestSlugValidator restSlugValidator;

    /**
     * Retrieves the ETag for the Project
     * <p>
     * This algorithm takes into account changes in Project Iterations as well.
     *
     * @param slug Project slug
     * @return calculated EntityTag
     * @throws NoSuchEntityException if project is obsolete or does not exist
     */
    public EntityTag generateTagForProject(String slug) {
        HProject project =
                restSlugValidator.retrieveAndCheckProject(slug, false);
        @SuppressWarnings("unchecked")
        List<Integer> iterationVersions =
                session.createQuery(
                        "select i.versionNum from HProjectIteration i "
                                +
                                "where i.project.slug =:slug and status not in (:statusList)")
                        .setParameter("slug", slug)
                        .setParameterList("statusList",
                                new Object[]{ OBSOLETE })
                        .setComment("ETagUtils.generateTagForProject-iteration")
                        .list();

        String hash =
                HashUtil.generateHash(project.getVersionNum() + ':'
                        + StringUtils.join(iterationVersions, ':'));

        return EntityTag.valueOf(hash);
    }

    /**
     * Retrieves the ETag for the ProjectIteration
     *
     * @param projectSlug   project slug
     * @param iterationSlug iteration slug
     * @return calculated EntityTag
     * @throw NoSuchEntityException if iteration is obsolete or does not exist
     */
    public EntityTag generateETagForIteration(String projectSlug,
            String iterationSlug) {
        HProjectIteration version = restSlugValidator
                .retrieveAndCheckIteration(projectSlug, iterationSlug, false);

        String hash = HashUtil.generateHash(String.valueOf(version.getVersionNum()));

        return EntityTag.valueOf(hash);
    }

    public EntityTag generateETagForDocument(HProjectIteration iteration,
            String id, Set<String> extensions) {
        HDocument doc = documentDAO.getByDocIdAndIteration(iteration, id);
        if (doc == null)
            throw new NoSuchEntityException("Document '" + id + "' not found.");

        Integer hashcode = 1;
        hashcode = hashcode * 31 + doc.getRevision();

        int extHash = 0;
        if (extensions.contains(PoHeader.ID)) {
            HPoHeader header = doc.getPoHeader();
            if (header != null) {
                extHash = header.getVersionNum();
            }
        }
        hashcode = hashcode * 31 + extHash;

        return EntityTag.valueOf(String.valueOf(hashcode));
    }

    public EntityTag generateETagForTranslatedDocument(
            HProjectIteration iteration, String docId, HLocale locale) {
        String stateHash =
                documentDAO.getTranslatedDocumentStateHash(iteration
                                .getProject().getSlug(), iteration.getSlug(), docId,
                        locale);
        return EntityTag.valueOf(stateHash);
    }
}
