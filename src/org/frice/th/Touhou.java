package org.frice.th;

import org.frice.Game;
import org.frice.anim.RotateAnim;
import org.frice.anim.move.*;
import org.frice.event.DelayedEvent;
import org.frice.obj.AttachedObjects;
import org.frice.obj.FObject;
import org.frice.obj.button.SimpleText;
import org.frice.obj.sub.ImageObject;
import org.frice.obj.sub.ShapeObject;
import org.frice.resource.graphics.ColorResource;
import org.frice.resource.image.FileImageResource;
import org.frice.resource.image.FrameImageResource;
import org.frice.resource.image.ImageResource;
import org.frice.th.obj.BloodedObject;
import org.frice.utils.BoolArray;
import org.frice.utils.EventManager;
import org.frice.utils.audio.AudioManager;
import org.frice.utils.audio.AudioPlayer;
import org.frice.utils.message.FLog;
import org.frice.utils.shape.FRectangle;
import org.frice.utils.time.FClock;
import org.frice.utils.time.FTimer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.frice.Initializer.launch;

public class Touhou extends Game {
	private int backgroundSpeed = 2;
	private static final int backgroundPicCountX = 3;
	private static final int backgroundPicCountY = 3;
	private static final int fastSpeed = 8;
	private BoolArray direction = new BoolArray(6);
	private int speed = fastSpeed;
	private int score = 0;
	private int life = 3;
	private AttachedObjects<FObject> playerEntity;
	private ImageObject player;
	private FTimer moveTimer = new FTimer(12);
	private FTimer checkTimer = new FTimer(3);
	private FTimer shootTimer = new FTimer(18);
	private FTimer enemyTimer = new FTimer(600);
	private FTimer enemyShootTimer = new FTimer(300);
	private static final int sceneWidth = 380;
	private List<BloodedObject> enemies = new LinkedList<>();
	private List<ImageObject> bullets = new LinkedList<>();
	private List<ImageObject> enemyBullets = new LinkedList<>();
	private List<ImageObject> backgroundImages;
	private ImageResource darkBackground;
	private ImageResource shineBackground;
	private SimpleText scoreText;
	private SimpleText lifeText;
	private EventManager eventManager = new EventManager();

	public Touhou() {
		// super(640, 480, 2);
		super(3);
	}

	@Override
	public void onInit() {
		AudioPlayer audioPlayer = AudioManager.getPlayer("./res/bgm.mp3");
		audioPlayer.start();
		setSize(640, 480);
		setAutoGC(true);
		setShowFPS(true);
		setMillisToRefresh(12);
		FLog.setLevel(FLog.ERROR);
		addKeyListener(null, event -> {
			speed = event.isShiftDown() ? 1 : fastSpeed;
			if (event.getKeyCode() >= KeyEvent.VK_LEFT && event.getKeyCode() <= KeyEvent.VK_DOWN)
				direction.set(event.getKeyCode() - KeyEvent.VK_LEFT, true);
			if (event.getKeyCode() == KeyEvent.VK_Z) direction.set(4, true);
			if (event.getKeyCode() == KeyEvent.VK_CONTROL) backgroundImages.forEach(o1 -> o1.setRes(shineBackground));
		}, event -> {
			speed = event.isShiftDown() ? 1 : fastSpeed;
			if (event.getKeyCode() >= KeyEvent.VK_LEFT && event.getKeyCode() <= KeyEvent.VK_DOWN)
				direction.set(event.getKeyCode() - KeyEvent.VK_LEFT, false);
			if (event.getKeyCode() == KeyEvent.VK_Z) direction.set(4, false);
			if (event.getKeyCode() == KeyEvent.VK_CONTROL) backgroundImages.forEach(o -> o.setRes(darkBackground));
		});
	}

	@Override
	public void onExit() {
		System.exit(0);
	}

	@Override
	public void onRefresh() {
		eventManager.check();
		if (shootTimer.ended() && direction.get(4) && !player.getDied()) {
			ImageObject bullet = bullet();
			bullets.add(bullet);
			addObject(1, bullet);
		}
		if (enemyTimer.ended()) for (int i = 0; i < Math.random() * 3; i++)
			addObject(1, enemy((int) (Math.log(FClock.getCurrent()) * 100)));
		if (enemyShootTimer.ended() && Math.random() < 0.3) enemies.forEach(e -> addObject(1, enemyBullet(e)));
		if (moveTimer.ended()) {
			//noinspection PointlessArithmeticExpression
			if (direction.get(KeyEvent.VK_LEFT - KeyEvent.VK_LEFT) && player.getX() > 10) playerEntity.move(-speed, 0);
			if (direction.get(KeyEvent.VK_RIGHT - KeyEvent.VK_LEFT) && player.getX() - player.getWidth() < sceneWidth)
				playerEntity.move(speed, 0);
			if (direction.get(KeyEvent.VK_UP - KeyEvent.VK_LEFT) && player.getY() > 10) playerEntity.move(0, -speed);
			if (direction.get(KeyEvent.VK_DOWN - KeyEvent.VK_LEFT) && player.getY() < getHeight() - player.getHeight() - 10)
				playerEntity.move(0, speed);
		}
		if (checkTimer.ended()) {
			enemies.removeIf(ImageObject::getDied);
			bullets.removeIf(ImageObject::getDied);
			scoreText.setText("Score: " + score);
			lifeText.setText("Life: " + life);
			enemies.forEach(e -> {
				bullets.forEach(b -> {
					if (e.collides(b)) {
						b.setDied(true);
						e.blood -= 200;
						if (e.blood <= 0) {
							e.setDied(true);
							score += 1;
						}
					}
				});
				if (e.collides(player)) e.setDied(true);
			});
			int enemyBulletSize = enemyBullets.size();
			enemyBullets.removeIf(b -> {
				if (b.collides(player)) {
					b.setDied(true);
					life--;
					return true;
				}
				return false;
			});
			if (enemyBulletSize != enemyBullets.size()) enemyBullets.removeIf(b -> {
				b.setDied(true);
				return true;
			});
			if (life < 0) {
				SimpleText gameOver = new SimpleText(ColorResource.RED, "Game Over", 100, 200);
				gameOver.setTextSize(100);
				addObject(2, gameOver);
				player.setDied(true);
				eventManager.insert(DelayedEvent.millisFromNow(1000, () -> {
					dialogShow("满身疮痍", "你鸡寄了");
					onExit();
				}));
			}
		}
	}

	@NotNull
	private ImageObject enemyBullet(BloodedObject e) {
		ImageResource bigImage = ImageResource.fromPath("./res/th11/enemy/enemy2.png");
		int rand = (int) (Math.random() * 4) * 32;
		// new FrameImageResource(IntStream.range(0, 8).mapToObj(x -> bigImage.part(x * 32, rand, 32, 32)).collect(Collectors.toList()), 50)
		ImageObject ret = new ImageObject(bigImage.part(0, rand, 32, 32), e.getX() + (e.getWidth() - 32) / 2, e.getY() + (e.getHeight() - 32) / 2);
		ret.addAnim(new ChasingMove(ret, player, 60));
		ret.addAnim(new AccurateMove(0, 300));
		ret.addAnim(new DirectedMove(ret, player.getX(), player.getY(), 100));
		enemyBullets.add(ret);
		return ret;
	}

	@NotNull
	private BloodedObject enemy(int blood) {
		ImageResource bigImage = ImageResource.fromPath("./res/th11/enemy/enemy.png");
		final int size = 32;
		final int num = (int) (Math.random() * 4);
		// ImageObject ret = new ImageObject(new FrameImageResource(IntStream.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)
		BloodedObject ret = new BloodedObject(new FrameImageResource(IntStream.of(0, 1, 2, 3, 2, 1).mapToObj(x -> bigImage.part(x * size, size * (8 + num), size, size)).collect(Collectors.toList()), 50), Math.random() * (sceneWidth - 2) + 2, 0, blood);
		ret.addAnim(new ApproachingMove(ret, player, 0.3));
		ret.addAnim(new AccurateMove(0, 200));
		enemies.add(ret);
		return ret;
	}

	@NotNull
	@Contract(pure = true)
	private ImageObject player() {
		ImageResource bigImage = ImageResource.fromPath("./res/th11/player/pl01/pl01.png");
		return new ImageObject(new FrameImageResource(IntStream.range(0, 8).mapToObj(x -> bigImage.part(x * 32, 0, 32, 48)).collect(Collectors.toList()), 50), (sceneWidth >>> 1) - 1, getHeight() - 50);
	}

	@NotNull
	@Contract(pure = true)
	private ImageObject bullet() {
		ImageResource bullet = new FileImageResource("./res/th11/player/pl01/pl01.png").part(16, 160, 16, 16);
		ImageObject object = new ImageObject(bullet, player.getX() + (player.getWidth() - bullet.getImage().getWidth()) / 2, player.getY());
		object.addAnim(new RotateAnim(3));
//		if (enemies.size() > 0) {
//			ImageObject enemy = enemies.stream().reduce((e1, e2) -> Math.abs(e1.getX() - player.getX()) + Math.abs(e1.getY() - player.getY()) < Math.abs(e2.getX() - player.getX()) * Math.abs(e2.getY() - player.getY()) ? e1 : e2).get();
//			object.addAnim(new DirectedMove(object, enemy.getX(), enemy.getY(), 1000));
//		} else
		object.addAnim(new AccurateMove(0, -1000));
		return object;
	}

	@Override
	public void onLastInit() {
		addObject(0, new ShapeObject(ColorResource.BLACK, new FRectangle(getWidth(), getHeight()), 0, 0));
		background();
		player = player();
		ShapeObject playerBox = new ShapeObject(ColorResource.DARK_GRAY, new FRectangle(2, 2), player.getX() + 15, player.getY() + 23);
		player.setCollisionBox(playerBox);
		playerEntity = new AttachedObjects<>(Arrays.asList(player, playerBox));
		addObject(1, player);
		addObject(2, new ShapeObject(ColorResource.八云紫, new FRectangle(getWidth(), 10)), new ShapeObject(ColorResource.八云紫, new FRectangle(10, getHeight())), new ShapeObject(ColorResource.八云紫, new FRectangle(getWidth(), getHeight()), 0, getHeight() - 10));
		double x = sceneWidth + player.getWidth() * 2;
		scoreText = new SimpleText(ColorResource.WHITE, "", x + 20, 100);
		lifeText = new SimpleText(ColorResource.WHITE, "", x + 20, 120);
		addObject(2, new ShapeObject(ColorResource.八云紫, new FRectangle(getWidth() - x, getHeight()), x, 0), scoreText, lifeText);
	}

	private void background() {
		backgroundImages = new ArrayList<>(backgroundPicCountX * backgroundPicCountY);
		darkBackground = new FileImageResource("./res/th11/background/stage04/stage04c.png");
		shineBackground = new FileImageResource("./res/th11/background/stage04/stage04b.png");
		for (int x = 0; x < backgroundPicCountX; x++)
			for (int y = 0; y < backgroundPicCountY; y++) {
				ImageObject object = new ImageObject(darkBackground, x * darkBackground.getImage().getWidth(), y * darkBackground.getImage().getHeight());
				object.addAnim(new CustomMove() {
					@Override
					public double getXDelta(double v) {
						return -v / (backgroundSpeed << 2) + (object.getX() < -object.getWidth() ? (object.getWidth() * backgroundPicCountX) : 0);
					}

					@Override
					public double getYDelta(double v) {
						return v / backgroundSpeed - (object.getY() > getHeight() ? object.getHeight() * backgroundPicCountY : 0);
					}
				});
				addObject(0, object);
				backgroundImages.add(object);
			}
	}

	public static void main(String[] args) {
		launch(Touhou.class);
	}
}
