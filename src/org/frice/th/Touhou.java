package org.frice.th;

import org.frice.Game;
import org.frice.Initializer;
import org.frice.anim.RotateAnim;
import org.frice.anim.move.AccurateMove;
import org.frice.anim.move.CustomMove;
import org.frice.obj.sub.ImageObject;
import org.frice.obj.sub.ShapeObject;
import org.frice.resource.graphics.ColorResource;
import org.frice.resource.image.FileImageResource;
import org.frice.resource.image.FrameImageResource;
import org.frice.resource.image.ImageResource;
import org.frice.utils.BoolArray;
import org.frice.utils.audio.AudioManager;
import org.frice.utils.audio.AudioPlayer;
import org.frice.utils.message.FLog;
import org.frice.utils.shape.FRectangle;
import org.frice.utils.time.FTimer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;
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
	private FTimer shootTimer = new FTimer(30);
	private FTimer enemyTimer = new FTimer(700);
	private static final int sceneWidth = 300;
	private List<ImageObject> enemies = new LinkedList<>();
	private List<ImageObject> bullets = new LinkedList<>();

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
		FLog.setLevel(FLog.ERROR);
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
		if (shootTimer.ended()) if (direction.get(4)) {
			ImageObject bullet = bullet();
			bullets.add(bullet);
			addObject(1, bullet);
		}
		if (enemyTimer.ended()) {
			ImageObject enemy = enemy();
			enemies.add(enemy);
			addObject(1, enemy);
			enemies.removeIf(ImageObject::getDied);
			bullets.removeIf(ImageObject::getDied);
		}
		enemies.forEach(e -> bullets.forEach(b -> {
			if (e.collides(b)) e.setDied(true);
		}));
		if (moveTimer.ended()) {
			if (direction.get(0) && player.getX() > 10) player.setX(player.getX() - speed);
			if (direction.get(KeyEvent.VK_RIGHT - KeyEvent.VK_LEFT) && player.getX() < sceneWidth)
				player.setX(player.getX() + speed);
			if (direction.get(KeyEvent.VK_UP - KeyEvent.VK_LEFT) && player.getY() > 10) player.setY(player.getY() - speed);
			if (direction.get(KeyEvent.VK_DOWN - KeyEvent.VK_LEFT) && player.getY() < getHeight() - player.getHeight() - 10)
				player.setY(player.getY() + speed);
		}
	}

	@NotNull
	@Contract(pure = true)
	private ImageObject enemy() {
		ImageResource bigImage = ImageResource.fromPath("./res/th11/enemy/enemy.png");
		final int size = 32;
		final int num = (int) (Math.random() * 4);
		// ImageObject ret = new ImageObject(new FrameImageResource(IntStream.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)
		ImageObject ret = new ImageObject(new FrameImageResource(IntStream.of(0, 1, 2, 3, 2, 1)
				.mapToObj(x -> bigImage.part(x * size, size * (8 + num), size, size))
				.collect(Collectors.toList()), 50), Math.random() * (sceneWidth - 2) + 2, 0);
		ret.addAnim(new AccurateMove(0, 100));
		return ret;
	}

	@NotNull
	@Contract(pure = true)
	private ImageObject player() {
		ImageResource bigImage = ImageResource.fromPath("./res/th11/player/pl01/pl01.png");
		return new ImageObject(new FrameImageResource(IntStream.range(0, 8)
				.mapToObj(x -> bigImage.part(x * 32, 0, 32, 48))
				.collect(Collectors.toList()), 50), (sceneWidth >>> 1) - 1, getHeight() - 50);
	}

	@NotNull
	@Contract(pure = true)
	private ImageObject bullet() {
		ImageResource bullet = new FileImageResource("./res/th11/player/pl01/pl01.png").part(16, 160, 16, 16);
		ImageObject object = new ImageObject(bullet, player.getX() + (player.getWidth() - bullet.getImage().getWidth()) / 2, player.getY());
		object.addAnim(new RotateAnim(3));
		object.addAnim(new AccurateMove(0, -900));
		return object;
	}

	@Override
	public void onLastInit() {
		addObject(0, new ShapeObject(ColorResource.BLACK, new FRectangle(getWidth(), getHeight()), 0, 0));
		background();
		addObject(1, player);
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
