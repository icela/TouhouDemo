package org.frice.th;

import kotlin.text.StringsKt;
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
import org.frice.th.self.GensokyoManager;
import org.frice.th.self.Marisa;
import org.frice.th.self.Reimu;
import org.frice.util.media.AudioPlayer;
import org.frice.util.shape.FRectangle;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lice.core.SymbolList;
import org.lice.model.ValueNode;

import java.awt.event.KeyEvent;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.frice.th.Touhou.sourceRoot;
import static org.frice.th.Touhou.stageWidth;

public class Stage {
	private @NotNull Touhou game;
	private @NotNull ImageObject playerPoint, playerPoint2;
	private static final int fastSpeed = 6;
	private @NotNull BitSet direction = new BitSet(6);
	private int speed = fastSpeed;
	private int score = 0;
	private int life;
	private int backgroundSpeed = 2;
	private static final int backgroundPicCountX = 5;
	private static final int backgroundPicCountY = 4;
	private @NotNull AttachedObjects player;
	private @NotNull ImageObject playerItself;
	private @NotNull GensokyoManager gensokyoManager;
	private @NotNull List<@NotNull BloodedObject> enemies = new LinkedList<>();
	private @NotNull List<@NotNull ImageObject> bullets = new LinkedList<>();
	private @NotNull List<@NotNull ImageObject> enemyBullets = new LinkedList<>();
	private @NotNull List<@NotNull ImageObject> backgroundImages;
	private @NotNull SimpleText scoreText, lifeText;
	private @NotNull ImageResource enemyBigImage;
	private AudioPlayer bgmPlayer;
	private @NotNull ImageResource darkBackground, shineBackground;
	public @NotNull SymbolList liceEnv;

	Stage(@NotNull Touhou touhou, @NotNull ImageResource enemyBigImage, @NotNull AudioPlayer bgmPlayer) {
		this.game = touhou;
		this.enemyBigImage = enemyBigImage;
		this.bgmPlayer = bgmPlayer;
		life = 2;
		liceEnv = new SymbolList();
		liceEnv.provideFunction("use-reimu-a", o -> gensokyoManager = new Reimu(game));
		liceEnv.provideFunction("use-marisa-a", o -> gensokyoManager = new Marisa(game));
		liceEnv.provideFunction("life", ls -> {
			if (!ls.isEmpty()) life = ((Number) ls.get(0)).intValue();
			return life;
		});
		liceEnv.provideFunction("assets-root", ls -> sourceRoot = ls.get(0).toString());
		liceEnv.defineFunction("run-later", (meta, nodes) -> {
			Number time = (Number) nodes.get(0).eval();
			if (time != null) game.runLater(time.longValue(), () -> {
				for (int i = 1; i < nodes.size(); i++) nodes.get(i).eval();
			});
			return new ValueNode(null, meta);
		});
		liceEnv.provideFunction("create-object", ls -> {
			BloodedObject ret = enemy(((Integer) ls.get(0)));
			enemies.add(ret);
			game.addObject(ret);
			return ret;
		});
		liceEnv.provideFunction("approach-player", ls -> {
			BloodedObject ret = (BloodedObject) ls.get(0);
			Number proportion = (Number) ls.get(1);
			ret.addAnim(new ApproachingMove(ret, playerItself, proportion.doubleValue()));
			return ret;
		});
		liceEnv.provideFunction("move", ls -> {
			BloodedObject ret = (BloodedObject) ls.get(0);
			Number x = (Number) ls.get(1);
			Number y = (Number) ls.get(2);
			ret.addAnim(new AccurateMove(x.doubleValue(), y.doubleValue()));
			return ret;
		});
		liceEnv.provideFunction("stop-anims", ls -> {
			ls.forEach(o -> ((FObject) o).stopAnims());
			return null;
		});
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

	public void enemyShoot() {
		enemies.forEach(e -> game.addObject(1, enemyBullet(e)));
	}

	private @NotNull ImageObject enemyBullet(BloodedObject e) {
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
		ret.setCollisionBox(ret.smallerBox(ret.getWidth() / 3));
		return ret;
	}

	@Contract(pure = true)
	private @NotNull ImageObject playerHitbox() {
		ImageResource image = game.createHitbox();
		return new ImageObject(image,
				playerItself.getX() + (playerItself.getWidth() - image.getImage().getWidth()) / 2,
				playerItself.getY() + (playerItself.getHeight() - image.getImage().getHeight()) / 2);
	}

	public void onPress(KeyEvent event) {
		dealWithShift(event.isShiftDown());
		if (event.getKeyCode() >= KeyEvent.VK_LEFT && event.getKeyCode() <= KeyEvent.VK_DOWN)
			direction.set(event.getKeyCode() - KeyEvent.VK_LEFT, true);
		if (event.getKeyCode() == KeyEvent.VK_Z) direction.set(4, true);
		if (event.getKeyCode() == KeyEvent.VK_CONTROL) {
			backgroundImages.forEach(o -> o.setRes(shineBackground));
		}
	}

	public void onRelease(KeyEvent event) {
		dealWithShift(event.isShiftDown());
		if (event.getKeyCode() >= KeyEvent.VK_LEFT && event.getKeyCode() <= KeyEvent.VK_DOWN)
			direction.set(event.getKeyCode() - KeyEvent.VK_LEFT, false);
		if (event.getKeyCode() == KeyEvent.VK_Z) direction.set(4, false);
		if (event.getKeyCode() == KeyEvent.VK_CONTROL) backgroundImages.forEach(o -> o.setRes(darkBackground));
	}

	private @NotNull BloodedObject enemy(int blood) {
		final int size = 32;
		final int num = (int) (Math.random() * 4);
		return new BloodedObject(new FrameImageResource(IntStream.of(0, 1, 2, 3, 2, 1)
				.mapToObj(x -> enemyBigImage.part(x * size, size * (8 + num), size, size))
				.collect(Collectors.toList()), 50), Math.random() * (stageWidth - 2) + 2, 0, blood);
	}

	public void shoot() {
		if (direction.get(4) && !playerItself.getDied()) {
			List<ImageObject> imageObjects = gensokyoManager.bullets();
			bullets.addAll(imageObjects);
			imageObjects.forEach(o -> game.addObject(1, o));
		}
	}

	public void move() {
		//noinspection PointlessArithmeticExpression
		if (direction.get(KeyEvent.VK_LEFT - KeyEvent.VK_LEFT)) {
			if (playerItself.getX() > 10) player.move(-speed, 0);
		}
		if (direction.get(KeyEvent.VK_RIGHT - KeyEvent.VK_LEFT)) {
			if (playerItself.getX() - playerItself.getWidth() < stageWidth) player.move(speed, 0);
		}
		if (direction.get(KeyEvent.VK_UP - KeyEvent.VK_LEFT) && playerItself.getY() > 10) player.move(0, -speed);
		if (direction.get(KeyEvent.VK_DOWN - KeyEvent.VK_LEFT) &&
				playerItself.getY() < game.getHeight() - playerItself.getHeight() - 10) player.move(0, speed);
	}

	public void check() {
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
					game.getHeight() - cirnoImage.getImage().getHeight());
			cirno.addAnim(new SimpleMove(cirnoImage.getImage().getWidth() * 1000 / timeToReallyDie, 0));
			game.addObject(2, cirno);
			game.runLater(timeToReallyDie, () -> {
				game.addObject(2, gameOver);
				cirno.stopAnims();
			});
			game.runLater(timeToReallyDie + 300, () -> {
				game.dialogShow("满身疮痍", "你鸡寄了");
				game.onExit();
			});
			playerItself.setDied(true);
		}
	}

	public void background() {
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
						return (-v / (backgroundSpeed << 2)) +
								((object.getX() < -object.getWidth()) ? (object.getWidth() * backgroundPicCountX) : 0);
					}

					@Override
					public double getYDelta(double v) {
						return (v / backgroundSpeed) -
								((object.getY() > game.getHeight()) ? (object.getHeight() * backgroundPicCountY) : 0);
					}
				});
				game.addObject(0, object);
				backgroundImages.add(object);
			}
	}

	public void start() {
		playerItself = gensokyoManager.player();
		playerPoint = playerHitbox();
		playerPoint.addAnim(new SimpleRotate(2));
		playerPoint.setVisible(false);
		playerPoint2 = playerHitbox();
		playerPoint2.addAnim(new SimpleRotate(-2));
		playerPoint2.setVisible(false);
		player = new AttachedObjects(Arrays.asList(playerItself, playerPoint, playerPoint2));
		playerItself.setCollisionBox(playerItself.smallerBox(22, 22, 13, 13));
		game.addObject(1, playerItself, playerPoint, playerPoint2);

		double x = stageWidth + playerItself.getWidth() * 2;
		scoreText = new SimpleText(ColorResource.WHITE, "", x + 20, 100);
		lifeText = new SimpleText(ColorResource.WHITE, "", x + 20, 120);
		if (Platforms.isOnWindows()) {
			scoreText.setFontName("Microsoft YaHei UI");
			lifeText.setFontName("Microsoft YaHei UI");
		}

		ShapeObject board = new ShapeObject(ColorResource.八云紫, new FRectangle(game.getWidth() - x, game.getHeight()), x, 0);
		game.addObject(2, board, scoreText, lifeText);
		bgmPlayer.start();
	}
}
