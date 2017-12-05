package org.frice.th;

import org.frice.Game;
import org.frice.Initializer;
import org.frice.anim.move.CustomMove;
import org.frice.obj.sub.ImageObject;
import org.frice.obj.sub.ShapeObject;
import org.frice.resource.graphics.ColorResource;
import org.frice.resource.image.FileImageResource;
import org.frice.resource.image.FrameImageResource;
import org.frice.resource.image.ImageResource;
import org.frice.utils.shape.FRectangle;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Touhou extends Game {
	private int backgroundSpeed = 2;
	private final int backgroundPicCountX = 3;
	private final int backgroundPicCountY = 3;

	public Touhou() {
		// super(640, 480, 2);
		super(3);
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
		ImageObject player = player();
		addObject(1, player);
	}

	@NotNull
	private ImageObject player() {
		ImageResource bigImage = new FileImageResource("./res/th11/player/pl00/pl00.png");
		FrameImageResource image = new FrameImageResource(IntStream.range(0, 8)
				.mapToObj(x -> bigImage.part(x * 32, 0, 32, 48)).collect(Collectors.toList()), 50);
		return new ImageObject(image, 0, 0);
	}

	private void background() {
		ImageResource image = new FileImageResource("./res/th11/background/stage04/stage04a.png");
		for (int x = 0; x < backgroundPicCountX; x++)
			for (int y = 0; y < backgroundPicCountY; y++) {
				ImageObject object = new ImageObject(image, x * image.getImage().getWidth(), y * image.getImage().getHeight());
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
