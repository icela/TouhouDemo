package org.frice.th;

import org.frice.Game;
import org.frice.anim.move.*;
import org.frice.anim.rotate.SimpleRotate;
import org.frice.event.DelayedEvent;
import org.frice.obj.AttachedObjects;
import org.frice.obj.button.SimpleText;
import org.frice.obj.sub.ImageObject;
import org.frice.obj.sub.ShapeObject;
import org.frice.resource.graphics.ColorResource;
import org.frice.resource.image.FileImageResource;
import org.frice.resource.image.FrameImageResource;
import org.frice.resource.image.ImageResource;
import org.frice.th.obj.BloodedObject;
import org.frice.util.media.AudioManager;
import org.frice.util.media.AudioPlayer;
import org.frice.util.message.FLog;
import org.frice.util.shape.FRectangle;
import org.frice.util.time.FClock;
import org.frice.util.time.FTimer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.PI;
import static org.frice.Initializer.launch;

public class Touhou extends Game {
	private int backgroundSpeed = 2;
	private static final int backgroundPicCountX = 3;
	private static final int backgroundPicCountY = 3;
	private static final int fastSpeed = 8;
	private BitSet direction = new BitSet(6);
	private int speed = fastSpeed;
	private int score = 0;
	private int life = 3;
	private AttachedObjects player;
	private ImageObject playerItself;
	private ImageObject playerPoint, playerPoint2;
	private FTimer moveTimer = new FTimer(12);
	private FTimer checkTimer = new FTimer(3);
	private FTimer shootTimer = new FTimer(36);
	private FTimer enemyTimer = new FTimer(500);
	private FTimer enemyShootTimer = new FTimer(200);
	private static final int sceneWidth = 380;
	private List<BloodedObject> enemies = new LinkedList<>();
	private List<ImageObject> bullets = new LinkedList<>();
	private List<ImageObject> enemyBullets = new LinkedList<>();
	private List<ImageObject> backgroundImages;
	private ImageResource darkBackground;
	private ImageResource shineBackground;
	private SimpleText scoreText;
	private SimpleText lifeText;
	private double angle = 0.0;
	private boolean useAngle = false;
	private ImageResource enemyBigImage;
	private ThreadPoolExecutor executor = new ThreadPoolExecutor(20, 60, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(20), new ThreadPoolExecutor.DiscardPolicy());

	public Touhou() {
		// super(640, 480, 2);
		super(3);
	}

	@Override
	public void onInit() {
		setSize(640, 480);
		setAutoGC(true);
		getLayers(0).setAutoGC(false);
		setShowFPS(true);
		setMillisToRefresh(12);
		FLog.setLevel(FLog.ERROR);
		addKeyListener(null, event -> {
			dealWithShift(event.isShiftDown());
			if (event.getKeyCode() >= KeyEvent.VK_LEFT && event.getKeyCode() <= KeyEvent.VK_DOWN)
				direction.set(event.getKeyCode() - KeyEvent.VK_LEFT, true);
			if (event.getKeyCode() == KeyEvent.VK_Z) direction.set(4, true);
			if (event.getKeyCode() == KeyEvent.VK_CONTROL) {
				backgroundImages.forEach(o1 -> o1.setRes(shineBackground));
				useAngle = !useAngle;
			}
		}, event -> {
			dealWithShift(event.isShiftDown());
			if (event.getKeyCode() >= KeyEvent.VK_LEFT && event.getKeyCode() <= KeyEvent.VK_DOWN)
				direction.set(event.getKeyCode() - KeyEvent.VK_LEFT, false);
			if (event.getKeyCode() == KeyEvent.VK_Z) direction.set(4, false);
			if (event.getKeyCode() == KeyEvent.VK_CONTROL) backgroundImages.forEach(o -> o.setRes(darkBackground));
		});
		playerItself = player();
		playerPoint = playerPoint();
		playerPoint.addAnim(new SimpleRotate(2));
		playerPoint.setVisible(false);
		playerPoint2 = playerPoint();
		playerPoint2.addAnim(new SimpleRotate(-2));
		playerPoint2.setVisible(false);
		player = new AttachedObjects(Arrays.asList(playerItself, playerPoint, playerPoint2));
		playerItself.setCollisionBox(playerItself.smallerBox(28, 15, 13, 13));
	}

	private void dealWithShift(boolean bool) {
		if (bool) {
			speed = 2;
			playerPoint.setVisible(true);
			playerPoint2.setVisible(true);
		} else {
			speed = fastSpeed;
			playerPoint.setVisible(false);
			playerPoint2.setVisible(false);
		}
	}

	@Override
	public void onExit() {
		System.exit(0);
	}

	@Override
	public void onRefresh() {
		if (shootTimer.ended() && direction.get(4) && !playerItself.getDied()) addObject(1, bullet());
		if (enemyTimer.ended())
			for (int i = 0; i < Math.random() * 3; i++) addObject(1, enemy((int) (Math.log(FClock.getCurrent()) * 100)));
		if (enemyShootTimer.ended() && Math.random() < 0.6) enemies.forEach(e -> addObject(1, enemyBullet(e)));
		while (angle > 3 * PI) angle -= (3 * PI);
		while (angle < 0) angle += (3 * PI);
		if (moveTimer.ended()) {
			//noinspection PointlessArithmeticExpression
			if (direction.get(KeyEvent.VK_LEFT - KeyEvent.VK_LEFT)) {
				if (playerItself.getX() > 10) player.move(-speed, 0);
				if (speed == fastSpeed && angle > PI / 2) angle -= 0.1;
			}
			if (direction.get(KeyEvent.VK_RIGHT - KeyEvent.VK_LEFT)) {
				if (playerItself.getX() - playerItself.getWidth() < sceneWidth) player.move(speed, 0);
				if (speed == fastSpeed && angle < PI + PI / 2) angle += 0.1;
			}
			if (direction.get(KeyEvent.VK_UP - KeyEvent.VK_LEFT) && playerItself.getY() > 10) player.move(0, -speed);
			if (direction.get(KeyEvent.VK_DOWN - KeyEvent.VK_LEFT) && playerItself.getY() < getHeight() - playerItself.getHeight() - 10)
				player.move(0, speed);
		}
		if (checkTimer.ended()) {
			enemies.removeIf(BloodedObject::getDied);
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
							executor.execute(AudioManager.getPlayer("./res/shake.mp3"));
							score += 1;
						}
					}
				});
				if (e.collides(playerItself)) e.setDied(true);
			});
			int enemyBulletSize = enemyBullets.size();
			enemyBullets.removeIf(b -> {
				if (playerItself.getDied()) return false;
				if (b.collides(playerItself)) {
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
			if (life < 0 && !playerItself.getDied()) {
				SimpleText gameOver = new SimpleText(ColorResource.RED, "Game Over", 100, 200);
				gameOver.setTextSize(100);
				addObject(2, gameOver);
				runLater(100, () -> IntStream.range(0, 10).forEach(i -> addObject(1, enemy(10))));
				runLater(200, () -> IntStream.range(0, 10).forEach(i -> addObject(1, enemy(10))));
				runLater(300, () -> IntStream.range(0, 10).forEach(i -> addObject(1, enemy(10))));
				runLater(400, () -> IntStream.range(0, 10).forEach(i -> addObject(1, enemy(10))));
				DelayedEvent event = DelayedEvent.millisFromNow(2500, () -> {
					dialogShow("满身疮痍", "你鸡寄了");
					onExit();
				});
				runLater(event);
				playerItself.setDied(true);
			}
		}
	}

	@NotNull
	private ImageObject enemyBullet(BloodedObject e) {
		ImageResource bigImage = ImageResource.fromPath("./res/th11/bullet/etama.png");
		int size = 16;
		int rand = (int) (Math.random() * 4) * size;
		// new FrameImageResource(IntStream.range(0, 8).mapToObj(x -> bigImage.part(x * 32, rand, 32, 32)).collect(Collectors.toList()), 50)
		ImageObject ret = new ImageObject(bigImage.part(rand, size << 1, size, size), e.getX() + (e.getWidth() - size) / 2, e.getY() + (e.getHeight() - size) / 2);
		ret.addAnim(new ChasingMove(ret, playerItself, 60));
		ret.addAnim(new AccurateMove(0, 300 * (e.getY() > playerItself.getY() ? -1 : 1)));
		ret.addAnim(new DirectedMove(ret, playerItself.getX(), playerItself.getY(), 50));
		enemyBullets.add(ret);
		ret.setCollisionBox(ret.smallerBox(2));
		return ret;
	}

	@NotNull
	private BloodedObject enemy(int blood) {
		final int size = 32;
		final int num = (int) (Math.random() * 4);
		// ImageObject ret = new ImageObject(new FrameImageResource(IntStream.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)
		BloodedObject ret = new BloodedObject(new FrameImageResource(IntStream.of(0, 1, 2, 3, 2, 1).mapToObj(x -> enemyBigImage.part(x * size, size * (8 + num), size, size)).collect(Collectors.toList()), 50), Math.random() * (sceneWidth - 2) + 2, 0, blood);
		ret.addAnim(new ApproachingMove(ret, playerItself, 0.3));
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
	private ImageObject playerPoint() {
		ImageResource image = ImageResource.fromPath("./res/th11/bullet/etama2.png").part(0, 16, 64, 64);
		return new ImageObject(image, playerItself.getX() + (playerItself.getWidth() - image.getImage().getWidth()) / 2, getHeight() - 50);
	}

	@NotNull
	private ImageObject[] bullet() {
		ImageResource pic = new FileImageResource("./res/th11/player/pl01/pl01.png");
		if (useAngle) {
			ImageResource bullet = pic.part(16, 160, 16, 16);
			ImageObject object = new ImageObject(bullet, playerItself.getX() + (playerItself.getWidth() - bullet.getImage().getWidth()) / 2, playerItself.getY() + playerItself.getHeight() / 2);
			ImageObject object2 = new ImageObject(bullet, playerItself.getX() + (playerItself.getWidth() - bullet.getImage().getWidth()) / 2, playerItself.getY() + playerItself.getHeight() / 2);
			SimpleRotate anim = new SimpleRotate(PI * 10);
			object.addAnim(anim);
			object2.addAnim(anim);
			object.addAnim(AccurateMove.byAngle(angle, 1000));
			object2.addAnim(AccurateMove.byAngle(angle + PI, 1000));
			bullets.add(object);
			bullets.add(object2);
			return new ImageObject[]{object, object2};
		} else {
			ImageResource bullet = pic.part(0, 144, 32, 16);
			ImageObject object = new ImageObject(bullet, playerItself.getX() + (playerItself.getWidth() - bullet.getImage().getWidth()) / 2, playerItself.getY());
			ImageObject object2 = new ImageObject(bullet, playerItself.getX() + (playerItself.getWidth() - bullet.getImage().getWidth()) / 2 + 20, playerItself.getY());
			ImageObject object3 = new ImageObject(bullet, playerItself.getX() + (playerItself.getWidth() - bullet.getImage().getWidth()) / 2 - 20, playerItself.getY());
			object.rotate(-PI / 2);
			object2.rotate(-PI / 2 + 0.2);
			object3.rotate(-PI / 2 - 0.2);
			object.addAnim(new AccurateMove(0, -1000));
			object2.addAnim(new AccurateMove(200, -1000));
			object3.addAnim(new AccurateMove(-200, -1000));
			bullets.add(object);
			bullets.add(object2);
			bullets.add(object3);
			return new ImageObject[]{object, object2, object3};
		}
	}

	@Override
	public void onLastInit() {
		enemyBigImage = ImageResource.fromPath("./res/th11/enemy/enemy.png");
		addObject(0, new ShapeObject(ColorResource.BLACK, new FRectangle(getWidth(), getHeight()), 0, 0));
		background();
		addObject(1, playerItself, playerPoint, playerPoint2);
		addObject(2, new ShapeObject(ColorResource.八云紫, new FRectangle(getWidth(), 10)), new ShapeObject(ColorResource.八云紫, new FRectangle(10, getHeight())), new ShapeObject(ColorResource.八云紫, new FRectangle(getWidth(), getHeight()), 0, getHeight() - 10));
		double x = sceneWidth + playerItself.getWidth() * 2;
		scoreText = new SimpleText(ColorResource.WHITE, "", x + 20, 100);
		lifeText = new SimpleText(ColorResource.WHITE, "", x + 20, 120);
		addObject(2, new ShapeObject(ColorResource.八云紫, new FRectangle(getWidth() - x, getHeight()), x, 0), scoreText, lifeText);
		AudioPlayer audioPlayer = AudioManager.getPlayer("./res/bgm.mp3");
		audioPlayer.start();
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
