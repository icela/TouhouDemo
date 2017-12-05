package org.frice.th;

import org.frice.Game;
import org.frice.Initializer;
import org.frice.anim.move.CustomMove;
import org.frice.obj.sub.ImageObject;
import org.frice.obj.sub.ShapeObject;
import org.frice.resource.graphics.ColorResource;
import org.frice.resource.image.FileImageResource;
import org.frice.resource.image.ImageResource;
import org.frice.utils.shape.FRectangle;

public class Touhou extends Game {
	private int backgroundSpeed = 2;
	private final int backgroundPicCountX = 3;
	private final int backgroundPicCountY = 3;

	public Touhou() {
		// super(640, 480, 2);
		super(2);
	}

	@Override
	public void onInit() {
		setSize(640, 480);
		setMillisToRefresh(10);
	}

	@Override
	public void onLastInit() {
		addObject(new ShapeObject(ColorResource.BLACK, new FRectangle(getWidth(), getHeight()), 0, 0));
		background();
	}

	private void background() {
		ImageResource ret = new FileImageResource("./res/th11/background/stage04/stage04a.png");
		for (int x = 0; x < backgroundPicCountX; x++)
			for (int y = 0; y < backgroundPicCountY; y++) {
				ImageObject object = new ImageObject(ret, x * ret.getImage().getWidth(), y * ret.getImage().getHeight());
				object.addAnim(new CustomMove() {
					@Override
					public double getXDelta(double v) {
						if (object.getX() < -object.getWidth())
							return -v / (backgroundSpeed << 2) + (object.getWidth() * backgroundPicCountX);
						return -v / (backgroundSpeed << 2);
					}

					@Override
					public double getYDelta(double v) {
						if (object.getY() > getHeight()) return v / backgroundSpeed - (object.getHeight() * backgroundPicCountY);
						return v / backgroundSpeed;
					}
				});
				addObject(0, object);
			}
	}

	public static void main(String[] args) {
		Initializer.launch(Touhou.class);
	}
}
