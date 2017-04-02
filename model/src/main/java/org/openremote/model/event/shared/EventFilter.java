/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.event.shared;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openremote.model.AttributeEvent;
import org.openremote.model.util.TextUtil;

/**
 * Filters {@link SharedEvent} by arbitrary criteria.
 */
@JsonSubTypes({
    @JsonSubTypes.Type(value = AttributeEvent.EntityIdFilter.class, name = "entity-id")
})
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "filterType"
)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    creatorVisibility= JsonAutoDetect.Visibility.NONE,
    getterVisibility= JsonAutoDetect.Visibility.NONE,
    setterVisibility= JsonAutoDetect.Visibility.NONE,
    isGetterVisibility= JsonAutoDetect.Visibility.NONE
)
public abstract class EventFilter<E extends SharedEvent> {

    public static String getFilterType(String className) {
        String type = TextUtil.toLowerCaseDash(className);
        if (type.length() > 7 && type.substring(type.length() - 7).equals("-filter"))
            type = type.substring(0, type.length() - 7);
        return type;
    }

    public static String getFilterType(Class<? extends EventFilter> filterClass) {
        return getFilterType(filterClass.getSimpleName());
    }

    public String getFilterType() {
        return getFilterType(getClass());
    }

    /**
     *
     * @return <code>null</code> if the filter doesn't match the given event.
     */
    public abstract E apply(E event);
}
