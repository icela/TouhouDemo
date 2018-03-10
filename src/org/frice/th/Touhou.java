package org.frice.th;

import org.frice.Game;
import org.frice.resource.image.ImageResource;
import org.frice.util.media.AudioManager;
import org.frice.util.message.FLog;
import org.frice.util.time.FTimer;
import org.jetbrains.annotations.NotNull;
import org.lice.Lice;
import org.lice.core.SymbolList;
import org.lice.model.MetaData;
import org.lice.model.ValueNode;

import java.io.File;
import java.nio.file.Paths;

import static org.frice.Initializer.launch;

public class Touhou extends Game {
	private FTimer moveTimer = new FTimer(12);
	private FTimer checkTimer = new FTimer(3);
	private FTimer shootTimer = new FTimer(54);
	private FTimer enemyShootTimer = new FTimer(200);
	public static int stageWidth = 600;
	public static String sourceRoot;
	private SymbolList liceEnv;
	private Stage stage;
	private ImageResource hitbox = ImageResource.fromPath(Touhou.sourceRoot + "/th11/bullet/etama2.png")
			.part(0, 16, 64, 64);

	public Touhou() {
		// super(640, 480, 2);
		super(5);
	}

	@Override
	public void onInit() {
		setSize(640 + 50, 560);
		setAutoGC(true);
		getLayers(0).setAutoGC(false);
		setShowFPS(true);
		FLog.setLevel(FLog.WARN);
		sourceRoot = "./res";
		stage = new Stage(this,
				ImageResource.fromPath(sourceRoot + "/th11/enemy/enemy.png"),
				AudioManager.getPlayer(sourceRoot + "/bgm.mp3"));
		liceEnv = stage.liceEnv;
		liceEnv.provideFunction("window-size", ls -> {
			setSize(((Number) ls.get(0)).intValue(), ((Number) ls.get(1)).intValue());
			return null;
		});
		liceEnv.provideFunction("stage-width", ls -> stageWidth = ((Number) ls.get(0)).intValue());
		liceEnv.provideFunction("millis->refresh", ls -> {
			setMillisToRefresh(((Number) ls.get(0)).intValue());
			return null;
		});
		liceEnv.provideFunction("title", ls -> {
			setTitle(ls.get(0).toString());
			return null;
		});
		liceEnv.defineVariable("simple", new ValueNode(null, new MetaData()));
		addKeyListener(null, stage::onPress, stage::onRelease);
		Lice.run(Paths.get("./lice/init.lice"), liceEnv);
	}


	@Override
	public void onExit() {
		System.exit(0);
	}

	@Override
	public void onRefresh() {
		if (shootTimer.ended()) stage.shoot();
		if (enemyShootTimer.ended() && Math.random() < 0.6) stage.enemyShoot();
		if (moveTimer.ended()) stage.move();
		if (checkTimer.ended()) stage.check();
	}

	public @NotNull ImageResource createHitbox() {
		return hitbox;
	}

	@Override
	public void onLastInit() {
		stage.start();
		Lice.run(new File("./lice/damuku.lice"), liceEnv);
	}


	public static void main(@NotNull String... args) {
		launch(Touhou.class);
	}
}

