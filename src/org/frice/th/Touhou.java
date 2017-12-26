package org.frice.th;

import kotlin.text.StringsKt;
import org.frice.Game;
import org.frice.anim.move.*;
import org.frice.anim.rotate.SimpleRotate;
import org.frice.obj.AttachedObjects;
import org.frice.obj.FObject;
import org.frice.obj.button.SimpleText;
import org.frice.obj.sub.ImageObject;
import org.frice.obj.sub.ShapeObject;
import org.frice.platform.Platforms;
import org.frice.resource.graphics.ColorResource;
import org.frice.resource.image.FileImageResource;
import org.frice.resource.image.FrameImageResource;
import org.frice.resource.image.ImageResource;
import org.frice.th.obj.BloodedObject;
import org.frice.util.media.AudioManager;
import org.frice.util.media.AudioPlayer;
import org.frice.util.message.FLog;
import org.frice.util.shape.FRectangle;
import org.frice.util.time.FTimer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lice.Lice;
import org.lice.core.SymbolList;
import org.lice.model.ValueNode;

import java.awt.event.KeyEvent;
import java.io.File;
import java.util.*;
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
	private int life;
	private AttachedObjects player;
	private ImageObject playerItself;
	private GensokyoManager gensokyoManager;
	private ImageObject playerPoint, playerPoint2;
	private FTimer moveTimer = new FTimer(12);
	private FTimer checkTimer = new FTimer(3);
	private FTimer shootTimer = new FTimer(54);
	private FTimer enemyShootTimer = new FTimer(200);
	static final int sceneWidth = 400;
	private List<BloodedObject> enemies = new LinkedList<>();
	private List<ImageObject> bullets = new LinkedList<>();
	private List<ImageObject> enemyBullets = new LinkedList<>();
	private List<ImageObject> backgroundImages;
	private ImageResource darkBackground, shineBackground;
	private SimpleText scoreText, lifeText;
	private double angle = 0.0;
	private ImageResource enemyBigImage;
	private AudioPlayer bgmPlayer;
	public static String sourceRoot;
	private SymbolList liceEnv;

	public Touhou() {
		// super(640, 480, 2);
		super(3);
	}

	@Override
	public void onInit() {
		setSize(640 + 50, 560);
		setAutoGC(true);
		getLayers(0).setAutoGC(false);
		setShowFPS(true);
		FLog.setLevel(FLog.WARN);
		life = 2;
		sourceRoot = "./res";
		liceEnv = SymbolList.with(symbolList -> {
			symbolList.provideFunction("use-reimu-a", o -> gensokyoManager = new GensokyoManager.Reimu(this));
			symbolList.provideFunction("use-marisa-a", o -> gensokyoManager = new GensokyoManager.Marisa(this));
			symbolList.provideFunction("millis-to-refresh", ls -> {
				setMillisToRefresh((Integer) ls.get(0));
				return null;
			});
			symbolList.provideFunction("title", ls -> {
				setTitle(ls.get(0).toString());
				return null;
			});
			symbolList.provideFunction("life", ls -> {
				if (!ls.isEmpty()) life = ((Number) ls.get(0)).intValue();
				return life;
			});
			symbolList.provideFunction("assets-root", ls -> sourceRoot = ls.get(0).toString());
			symbolList.defineFunction("run-later", ((metaData, nodes) -> {
				Number time = (Number) nodes.get(0).eval();
				if (time != null) runLater(time.longValue(), () -> {
					for (int i = 0; i < nodes.size(); i++) {
						if (i != 0) nodes.get(i).eval();
					}
				});
				return new ValueNode(null, metaData);
			}));
			symbolList.provideFunction("create-object", ls -> {
				BloodedObject ret = enemy(((Integer) ls.get(0)));
				enemies.add(ret);
				addObject(ret);
				return ret;
			});
			symbolList.provideFunction("approach-player", ls -> {
				BloodedObject ret = (BloodedObject) ls.get(0);
				Number proportion = (Number) ls.get(1);
				ret.addAnim(new ApproachingMove(ret, playerItself, proportion.doubleValue()));
				return ret;
			});
			symbolList.provideFunction("move", ls -> {
				BloodedObject ret = (BloodedObject) ls.get(0);
				Number x = (Number) ls.get(1);
				Number y = (Number) ls.get(2);
				ret.addAnim(new AccurateMove(x.doubleValue(), y.doubleValue()));
				return ret;
			});
			symbolList.provideFunction("stop-anims", ls -> {
				ls.forEach(o -> ((FObject) o).stopAnims());
				return null;
			});
		});
		addKeyListener(null, event -> {
			dealWithShift(event.isShiftDown());
			if (event.getKeyCode() >= KeyEvent.VK_LEFT && event.getKeyCode() <= KeyEvent.VK_DOWN)
				direction.set(event.getKeyCode() - KeyEvent.VK_LEFT, true);
			if (event.getKeyCode() == KeyEvent.VK_Z) direction.set(4, true);
			if (event.getKeyCode() == KeyEvent.VK_CONTROL) {
				backgroundImages.forEach(o -> o.setRes(shineBackground));
			}
		}, event -> {
			dealWithShift(event.isShiftDown());
			if (event.getKeyCode() >= KeyEvent.VK_LEFT && event.getKeyCode() <= KeyEvent.VK_DOWN)
				direction.set(event.getKeyCode() - KeyEvent.VK_LEFT, false);
			if (event.getKeyCode() == KeyEvent.VK_Z) direction.set(4, false);
			if (event.getKeyCode() == KeyEvent.VK_CONTROL) backgroundImages.forEach(o -> o.setRes(darkBackground));
		});
		Lice.run(new File("./lice/init.lice"), liceEnv);
		playerItself = gensokyoManager.player();
		playerPoint = playerHitbox();
		playerPoint.addAnim(new SimpleRotate(2));
		playerPoint.setVisible(false);
		playerPoint2 = playerHitbox();
		playerPoint2.addAnim(new SimpleRotate(-2));
		playerPoint2.setVisible(false);
		player = new AttachedObjects(Arrays.asList(playerItself, playerPoint, playerPoint2));
		playerItself.setCollisionBox(playerItself.smallerBox(28, 15, 13, 13));
		bgmPlayer = AudioManager.getPlayer(sourceRoot + "/bgm.mp3");
		enemyBigImage = ImageResource.fromPath(sourceRoot + "/th11/enemy/enemy.png");
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
		if (shootTimer.ended() && direction.get(4) && !playerItself.getDied()) bullet().forEach(o -> addObject(1, o));
		//if (enemyTimer.ended()) for (int i = 0; i < Math.random() * 3; i++) addObject(1, enemy((int) (Math.log(FClock
		//		.getCurrent()) * 100)));
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
			if (direction.get(KeyEvent.VK_DOWN - KeyEvent.VK_LEFT) &&
					playerItself.getY() < getHeight() - playerItself.getHeight() - 10) player.move(0, speed);
		}
		if (checkTimer.ended()) {
			enemies.removeIf(BloodedObject::getDied);
			bullets.removeIf(ImageObject::getDied);
			scoreText.setText("Score: " + score);
			if (life > 0) lifeText.setText("Life: " + StringsKt.repeat("★", life));
			enemies.forEach(e -> {
				bullets.removeIf(b -> {
					if (e.collides(b)) {
						gensokyoManager.dealWithBullet(b);
						b.addAnim(new SimpleMove(0, 500));
						e.blood -= 200;
						if (e.blood <= 0) {
							e.setDied(true);
							score += 1;
						}
						return true;
					}
					return false;
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
				final int timeToReallyDie = 1200;
				ImageResource cirnoImage = ImageResource.fromPath(sourceRoot + "/die-cirno.png");
				ImageObject cirno = new ImageObject(cirnoImage,
						-cirnoImage.getImage().getWidth(),
						getHeight() - cirnoImage.getImage().getHeight());
				cirno.addAnim(new SimpleMove(cirnoImage.getImage().getWidth() * 1000 / timeToReallyDie, 0));
				addObject(2, cirno);
				runLater(timeToReallyDie, () -> {
					addObject(2, gameOver);
					cirno.stopAnims();
				});
				runLater(timeToReallyDie + 300, () -> {
					dialogShow("满身疮痍", "你鸡寄了");
					onExit();
				});
				playerItself.setDied(true);
			}
		}
	}

	@NotNull
	private ImageObject enemyBullet(BloodedObject e) {
		ImageResource bigImage = ImageResource.fromPath(sourceRoot + "/th11/bullet/etama.png");
		int size = 16;
		int rand = (int) (Math.random() * 4) * size;
		// new FrameImageResource(IntStream.range(0, 8).mapToObj(x -> bigImage.part(x * 32, rand, 32, 32)).collect(Collectors.toList()), 50)
		ImageObject ret = new ImageObject(bigImage.part(rand, size << 1, size, size),
				e.getX() + (e.getWidth() - size) / 2,
				e.getY() + (e.getHeight() - size) / 2);
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
		return new BloodedObject(new FrameImageResource(IntStream.of(0, 1, 2, 3, 2, 1)
				.mapToObj(x -> enemyBigImage.part(x * size, size * (8 + num), size, size))
				.collect(Collectors.toList()), 50), Math.random() * (sceneWidth - 2) + 2, 0, blood);
	}

	@NotNull
	@Contract(pure = true)
	private ImageObject playerHitbox() {
		ImageResource image = ImageResource.fromPath(sourceRoot + "/th11/bullet/etama2.png").part(0, 16, 64, 64);
		return new ImageObject(image,
				playerItself.getX() + (playerItself.getWidth() - image.getImage().getWidth()) / 2,
				getHeight() - 50);
	}

	private List<ImageObject> bullet() {
		List<ImageObject> imageObjects = gensokyoManager.bullets();
		bullets.addAll(imageObjects);
		return imageObjects;
	}

	@Override
	public void onLastInit() {
		addObject(0, new ShapeObject(ColorResource.BLACK, new FRectangle(getWidth(), getHeight()), 0, 0));
		background();
		addObject(1, playerItself, playerPoint, playerPoint2);
		addObject(2,
				new ShapeObject(ColorResource.八云紫, new FRectangle(getWidth(), 10)),
				new ShapeObject(ColorResource.八云紫, new FRectangle(10, getHeight())),
				new ShapeObject(ColorResource.八云紫, new FRectangle(getWidth(), getHeight()), 0, getHeight() - 10));
		double x = sceneWidth + playerItself.getWidth() * 2;
		scoreText = new SimpleText(ColorResource.WHITE, "", x + 20, 100);
		lifeText = new SimpleText(ColorResource.WHITE, "", x + 20, 120);
		if (Platforms.isOnWindows()) {
			scoreText.setFontName("Microsoft YaHei UI");
			lifeText.setFontName("Microsoft YaHei UI");
		}
		addObject(2,
				new ShapeObject(ColorResource.八云紫, new FRectangle(getWidth() - x, getHeight()), x, 0),
				scoreText,
				lifeText);
		bgmPlayer.start();
		Lice.run(new File("./lice/damuku.lice"), liceEnv);
	}

	private void background() {
		backgroundImages = new ArrayList<>(backgroundPicCountX * backgroundPicCountY);
		darkBackground = new FileImageResource(sourceRoot + "/th11/background/stage04/stage04c.png");
		shineBackground = new FileImageResource(sourceRoot + "/th11/background/stage04/stage04b.png");
		for (int x = 0; x < backgroundPicCountX; x++)
			for (int y = 0; y < backgroundPicCountY; y++) {
				ImageObject object = new ImageObject(darkBackground,
						x * darkBackground.getImage().getWidth(),
						y * darkBackground.getImage().getHeight());
				object.addAnim(new CustomMove() {
					@Override
					public double getXDelta(double v) {
						return -v / (backgroundSpeed << 2) +
								(object.getX() < -object.getWidth() ? (object.getWidth() * backgroundPicCountX) : 0);
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

