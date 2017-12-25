package org.frice.th.obj;

import org.frice.obj.sub.ImageObject;
import org.frice.resource.image.ImageResource;
import org.jetbrains.annotations.NotNull;

public class BloodedObject extends ImageObject {
	public int blood;

	public BloodedObject(@NotNull ImageResource res, double x, double y, int blood) {
		super(res, x, y);
		this.blood = blood;
	}
}
