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
import org.frice.utils.time.FTimer;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Touhou extends Game {
	private int backgroundSpeed = 2;
	private final int backgroundPicCountX = 3;
	private final int backgroundPicCountY = 3;
	private int direction = 0;
	private int pressedKeyCount = 0;
	private int speed = 6;
	private ImageObject player = player();
	private FTimer moveTimer = new FTimer(6);

	public Touhou() {
		// super(640, 480, 2);
		super(3);
	}

	@Override
	public void onInit() {
		setSize(640, 480);
		setAutoGC(false);
		setMillisToRefresh(10);
		Consumer<KeyEvent> a = e -> {
			direction = e.getKeyCode();
			if (e.getKeyCode() >= KeyEvent.VK_LEFT && e.getKeyCode() <= KeyEvent.VK_DOWN) pressedKeyCount++;
			speed = e.isShiftDown() ? 1 : 6;
		};
		addKeyListener(a, a, e -> {
			if (e.getKeyCode() >= KeyEvent.VK_LEFT && e.getKeyCode() <= KeyEvent.VK_DOWN) pressedKeyCount--;
			if (pressedKeyCount == 0) direction = 0;
		});
	}

	@Override
	public void onRefresh() {
		if (moveTimer.ended()) {
			if (direction == KeyEvent.VK_LEFT && player.getX() > 10) player.setX(player.getX() - speed);
			else if (direction == KeyEvent.VK_RIGHT && player.getX() < 300) player.setX(player.getX() + speed);
			else if (direction == KeyEvent.VK_UP && player.getY() > 10) player.setY(player.getY() - speed);
			else if (direction == KeyEvent.VK_DOWN && player.getY() < getHeight() - player.getHeight() - 10)
				player.setY(player.getY() + speed);
		}
	}

	@Override
	public void onLastInit() {
		addObject(0, new ShapeObject(ColorResource.BLACK, new FRectangle(getWidth(), getHeight()), 0, 0));
		background();
		addObject(1, player);
	}

	@NotNull
	private ImageObject player() {
		ImageResource bigImage = new FileImageResource("./res/th11/player/pl00/pl00.png");
		return new ImageObject(new FrameImageResource(IntStream.range(0, 8)
				.mapToObj(x -> bigImage.part(x * 32, 0, 32, 48)).collect(Collectors.toList()), 50), 0, 0);
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
//						return 0;
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
