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
package org.zanata.dao;

import javax.enterprise.context.RequestScoped;
import org.zanata.model.security.HCredentials;
import org.zanata.model.security.HSSOCredentials;

/**
 * @author Carlos Munoz <a
 *         href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@RequestScoped
public class CredentialsDAO extends AbstractDAOImpl<HCredentials, Long> {
    public CredentialsDAO() {
        super(HCredentials.class);
    }

    /**
     * Finds a Credentials instance by the user. NOTE: The user passed in is not
     * the account user, is the user stored under the credentials.
     *
     * @param user
     *            The user registered under the credentials object.
     * @return A Credentials object for the given user, or null if such an
     *         object could not be found.
     */
    public HCredentials findByUser(String user) {
        return (HCredentials) getSession()
                .createQuery("from HCredentials c where c.user = :user")
                .setParameter("user", user)
                .setComment("CredentialsDAO.findByUser").uniqueResult();
    }

    /**
     * Find a SSO Credentials instance by the user (e.g. uuid in the Red Hat case).
     * @param user
     * @return a HSSOCredentials object
     */
    public HSSOCredentials findSSOUser(String user) {
        return (HSSOCredentials) getSession()
                .createQuery("from HSSOCredentials c where c.user = :user")
                .setParameter("user", user)
                .setComment("CredentialsDAO.findSSOUser").uniqueResult();
    }
}
