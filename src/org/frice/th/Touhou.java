package org.frice.th;

import org.frice.Game;
import org.frice.Initializer;
import org.frice.anim.move.CustomMove;
import org.frice.anim.move.SimpleMove;
import org.frice.obj.sub.ImageObject;
import org.frice.obj.sub.ShapeObject;
import org.frice.resource.graphics.ColorResource;
import org.frice.resource.image.FileImageResource;
import org.frice.resource.image.FrameImageResource;
import org.frice.resource.image.ImageResource;
import org.frice.utils.BoolArray;
import org.frice.utils.message.FLog;
import org.frice.utils.shape.FRectangle;
import org.frice.utils.time.FTimer;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Touhou extends Game {
	private int backgroundSpeed = 2;
	private static final int backgroundPicCountX = 3;
	private static final int backgroundPicCountY = 3;
	private static final int fastSpeed = 5;
	private BoolArray direction = new BoolArray(5);
	private int speed = fastSpeed;
	private ImageObject player = player();
	private FTimer moveTimer = new FTimer(10);

	public Touhou() {
		// super(640, 480, 2);
		super(3);
	}

	@Override
	public void onInit() {
		setSize(640, 480);
		setAutoGC(true);
		setShowFPS(false);
		setMillisToRefresh(10);
		FLog.INSTANCE.setLevel(FLog.ERROR);
		Consumer<KeyEvent> a = e -> {
			if (e.getKeyCode() >= KeyEvent.VK_LEFT && e.getKeyCode() <= KeyEvent.VK_DOWN)
				direction.set(e.getKeyCode() - KeyEvent.VK_LEFT, true);
			speed = e.isShiftDown() ? 1 : 3;
			if (e.getKeyCode() == KeyEvent.VK_Z) direction.set(4, true);
		};
		addKeyListener(a, a, e -> {
			if (e.getKeyCode() >= KeyEvent.VK_LEFT && e.getKeyCode() <= KeyEvent.VK_DOWN)
				direction.set(e.getKeyCode() - KeyEvent.VK_LEFT, false);
			if (e.getKeyCode() == KeyEvent.VK_Z) direction.set(4, false);
		});
	}

	@Override
	public void onRefresh() {
		if (moveTimer.ended()) {
			if (direction.get(0) && player.getX() > 10) player.setX(player.getX() - speed);
			if (direction.get(KeyEvent.VK_RIGHT - KeyEvent.VK_LEFT) && player.getX() < 300)
				player.setX(player.getX() + speed);
			if (direction.get(KeyEvent.VK_UP - KeyEvent.VK_LEFT) && player.getY() > 10) player.setY(player.getY() - speed);
			if (direction.get(KeyEvent.VK_DOWN - KeyEvent.VK_LEFT) && player.getY() < getHeight() - player.getHeight() - 10)
				player.setY(player.getY() + speed);
			if (direction.get(4)) {
				addObject(1, bullet());
			}
		}
	}

	private ImageObject bullet() {
		ImageResource bullet = new FileImageResource("./res/th11/player/pl01/pl01.png").part(16, 160, 16, 16);
		ImageObject object = new ImageObject(bullet, player.getX() + (player.getWidth() - bullet.getImage().getWidth()) / 2, player.getY());
		// object.addAnim(new RotateAnim(10));
		object.addAnim(new SimpleMove(0, -900));
		return object;
	}

	@Override
	public void onLastInit() {
		addObject(0, new ShapeObject(ColorResource.BLACK, new FRectangle(getWidth(), getHeight()), 0, 0));
		background();
		addObject(1, player);
	}

	@NotNull
	private ImageObject player() {
		ImageResource bigImage = new FileImageResource("./res/th11/player/pl01/pl01.png");
		return new ImageObject(new FrameImageResource(IntStream.range(0, 8)
				.mapToObj(x -> bigImage.part(x * 32, 0, 32, 48)).collect(Collectors.toList()), 50), 0, 0);
	}

	private void background() {
		ImageResource image = new FileImageResource("./res/th11/background/stage04/stage04c.png");
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
