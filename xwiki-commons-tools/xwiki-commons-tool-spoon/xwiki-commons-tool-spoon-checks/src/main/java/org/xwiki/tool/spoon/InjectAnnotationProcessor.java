/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
package org.xwiki.tool.spoon;

import java.lang.annotation.Annotation;
import java.util.List;

import spoon.processing.Property;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;

/**
 * Verifies that the {@code @Inject} annotation is only used with interfaces, except for 2 cases. They are:
 * <ul>
 *   <li>the field type is excluded by the user</li>
 *   <li>the field type points to a Component implementation that defines its role as itself using the "roles"
 *       attribute of the {@code @Component} annotation</li>
 * </ul>
 *
 * @version $Id$
 * @since 12.4RC1
 */
public class InjectAnnotationProcessor extends AbstractXWikiProcessor<CtAnnotation<? extends Annotation>>
{
    @Property
    private List<String> excludedFieldTypes;

    @Override
    public void process(CtAnnotation<? extends Annotation> ctAnnotation)
    {
        if (ctAnnotation.getAnnotationType().getQualifiedName().equals("javax.inject.Inject")) {
            CtElement element = ctAnnotation.getAnnotatedElement();
            if (element instanceof CtField) {
                CtField<?> ctField = (CtField<?>) element;
                // We need to handle the special case when we use the same class as both the component interface and
                // component implementation, using for ex: "@Component(roles = ComponentAndInterface.class)".
                if (!isExcluded(ctField) && ctField.getType().isClass()
                    && !isInterfaceAndImplementationCombined(ctField))
                {
                    registerError(String.format("Only interfaces should have the @Inject annotation but got [%s] which "
                        + "is a class. Problem at [%s]", ctField.getType().getTypeDeclaration().getQualifiedName(),
                        ctAnnotation.getPosition()));
                }
            } else {
                registerError(String.format("Only fields should use the @Inject annotation. Problem at [%s]",
                    ctAnnotation.getPosition()));
            }
        }
    }

    private boolean isExcluded(CtField<?> ctField)
    {
        return this.excludedFieldTypes == null ? false
            : this.excludedFieldTypes.contains(ctField.getType().getQualifiedName());
    }

    private boolean isInterfaceAndImplementationCombined(CtField<?> field)
    {
        boolean isValid = false;
        if (field.getType().getTypeDeclaration() instanceof CtClass<?>) {
            CtClass<?> ctClass = (CtClass<?>) field.getType().getTypeDeclaration();
            CtAnnotation<? extends Annotation> componentAnnotation = getComponentAnnotation(ctClass);
            if (componentAnnotation != null) {
                CtElement ctElement = componentAnnotation.getValue("roles");
                if (ctElement != null && ctElement.getReferencedTypes().contains(field.getType())) {
                    isValid = true;
                }
            }
        }
        return isValid;
    }

    private CtAnnotation<? extends Annotation> getComponentAnnotation(CtClass<?> ctClass)
    {
        CtAnnotation<? extends Annotation> result = null;
        for (CtAnnotation<? extends Annotation> ctAnnotation
            : SpoonUtils.getAnnotationsIncludingFromSuperclasses(ctClass))
        {
            if (ctAnnotation.getType().getQualifiedName().equals("org.xwiki.component.annotation.Component")) {
                result = ctAnnotation;
                break;
            }
        }
        return result;
    }
}
