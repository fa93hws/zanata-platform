/*
 * Copyright 2017, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.rest.review.dto;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.zanata.model.IssuePriority;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransReviewCriteria {
    private Long id;
    private IssuePriority priority;
    private String description;
    private boolean editable;

    public TransReviewCriteria() {
    }

    public TransReviewCriteria(Long id, IssuePriority priority, String description,
            boolean editable) {
        this.id = id;
        this.priority = priority;
        this.description = description;
        this.editable = editable;
    }


    public IssuePriority getPriority() {
        return priority;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEditable() {
        return editable;
    }

    public Long getId() {
        return id;
    }
}
