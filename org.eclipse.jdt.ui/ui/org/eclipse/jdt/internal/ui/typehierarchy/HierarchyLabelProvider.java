/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetFilter;

/**
 * Label provider for the hierarchy viewers. Types in the hierarchy that are not belonging to the
 * input scope are rendered differntly.
  */
public class HierarchyLabelProvider extends AppearanceAwareLabelProvider {

	private static class FocusDescriptor extends CompositeImageDescriptor {
		private ImageDescriptor fBase;
		public FocusDescriptor(ImageDescriptor base) {
			fBase= base;
		}
		protected void drawCompositeImage(int width, int height) {
			drawImage(fBase.getImageData(), 0, 0);
			drawImage(JavaPluginImages.DESC_OVR_FOCUS.getImageData(), 0, 0);
		}
		protected Point getSize() {
			return JavaElementImageProvider.BIG_SIZE;
		}
		public int hashCode() {
			return fBase.hashCode();
		}
		public boolean equals(Object object) {
			return object != null && FocusDescriptor.class.equals(object.getClass()) && ((FocusDescriptor)object).fBase.equals(fBase);
		}		
	}

	private Color fGrayedColor;
	private Color fSpecialColor;

	private ViewerFilter fFilter;
	
	private TypeHierarchyLifeCycle fHierarchy;

	public HierarchyLabelProvider(TypeHierarchyLifeCycle lifeCycle) {
		super(DEFAULT_TEXTFLAGS, DEFAULT_IMAGEFLAGS);
		fHierarchy= lifeCycle;
		fFilter= null;
		addLabelDecorator(new HierarchyOverrideIndicatorLabelDecorator(lifeCycle));
	}
				

	/**
	 * @return Returns the filter.
	 */
	public ViewerFilter getFilter() {
		return fFilter;
	}

	/**
	 * @param filter The filter to set.
	 */
	public void setFilter(ViewerFilter filter) {
		fFilter= filter;
	}

	protected boolean isDifferentScope(IType type) {
		if (fFilter != null && !fFilter.select(null, null, type)) {
			return true;
		}
		
		IJavaElement input= fHierarchy.getInputElement();
		if (input == null || input.getElementType() == IJavaElement.TYPE) {
			return false;
		}
			
		IJavaElement parent= type.getAncestor(input.getElementType());
		if (input.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
			if (parent == null || parent.getElementName().equals(input.getElementName())) {
				return false;
			}
		} else if (input.equals(parent)) {
			return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see ILabelProvider#getText
	 */ 	
	public String getText(Object element) {
		String text= super.getText(element);
		return decorateText(text, element);
	}	
	
	
	/* (non-Javadoc)
	 * @see ILabelProvider#getImage
	 */ 
	public Image getImage(Object element) {
		Image result= null;
		if (element instanceof IType) {
			ImageDescriptor desc= getTypeImageDescriptor((IType) element);
			if (desc != null) {
				if (element.equals(fHierarchy.getInputElement())) {
					desc= new FocusDescriptor(desc);
				}
				result= JavaPlugin.getImageDescriptorRegistry().get(desc);
			}
		} else {
			result= fImageLabelProvider.getImageLabel(element, evaluateImageFlags(element));
		}
		return decorateImage(result, element);
	}

	private ImageDescriptor getTypeImageDescriptor(IType type) {
		ITypeHierarchy hierarchy= fHierarchy.getHierarchy();
		if (hierarchy == null) {
			return new JavaElementImageDescriptor(JavaPluginImages.DESC_OBJS_CLASS, 0, JavaElementImageProvider.BIG_SIZE);
		}
		
		IType originalType= (IType) JavaModelUtil.toOriginal(type);
		int flags= hierarchy.getCachedFlags(originalType);
		if (flags == -1) {
			return new JavaElementImageDescriptor(JavaPluginImages.DESC_OBJS_CLASS, 0, JavaElementImageProvider.BIG_SIZE);
		}
		
		boolean isInterface= Flags.isInterface(flags);
		boolean isInner= (type.getDeclaringType() != null);
		ImageDescriptor desc;
		if (isDifferentScope(type)) {
			desc= isInterface ? JavaPluginImages.DESC_OBJS_INTERFACEALT : JavaPluginImages.DESC_OBJS_CLASSALT;
		} else {
			desc= JavaElementImageProvider.getTypeImageDescriptor(isInterface, isInner, flags);
		}
		int adornmentFlags= 0;
		if (Flags.isFinal(flags)) {
			adornmentFlags |= JavaElementImageDescriptor.FINAL;
		}
		if (Flags.isAbstract(flags) && !isInterface) {
			adornmentFlags |= JavaElementImageDescriptor.ABSTRACT;
		}
		if (Flags.isStatic(flags)) {
			adornmentFlags |= JavaElementImageDescriptor.STATIC;
		}
		
		return new JavaElementImageDescriptor(desc, adornmentFlags, JavaElementImageProvider.BIG_SIZE);
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	public Color getForeground(Object element) {
		if (element instanceof IMethod) {
			if (fSpecialColor == null) {
				fSpecialColor= Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE);
			}
			return fSpecialColor;
		} else if (element instanceof IType && isDifferentScope((IType) element)) {
			if (fGrayedColor == null) {
				fGrayedColor= Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
			}
			return fGrayedColor;
		}
		return null;
	}	
	
	

}
