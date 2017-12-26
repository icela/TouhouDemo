package org.frice.th.self;

import org.frice.anim.move.AccurateMove;
import org.frice.anim.rotate.SimpleRotate;
import org.frice.obj.sub.ImageObject;
import org.frice.resource.image.FrameImageResource;
import org.frice.resource.image.ImageResource;
import org.frice.th.Touhou;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.frice.th.self.GensokyoManager.DefaultImpl.makeLeftRightBullets;

public class Reimu implements GensokyoManager {
	private @NotNull Touhou game;
	private @NotNull String REIMU_RES = Touhou.sourceRoot + "/th11/player/pl00/pl00.png";
	private @NotNull ImageResource bigImage = ImageResource.fromPath(REIMU_RES);
	private @NotNull ImageResource mainBullet = bigImage.part(0, 176, 64, 16);
	private @NotNull List<@NotNull ImageResource> afterUsedBullet = IntStream.of(0, 1, 3)
			.mapToObj(i -> bigImage.part(i * 8, 144, 16, 16))
			.collect(Collectors.toList());
	private @NotNull ImageObject player;

	public Reimu(@NotNull Touhou game) {
		this.game = game;
		player = new ImageObject(new FrameImageResource(IntStream.range(0, 8)
				.mapToObj(x -> bigImage.part(x * 32, 0, 32, 48))
				.collect(Collectors.toList()), 50), (Touhou.stageWidth >>> 1) - 1, game.getHeight() - 50);
	}

	@Override
	public @NotNull List<@NotNull ImageObject> bullets() {
		return makeLeftRightBullets(player, mainBullet);
	}

	@Override
	public void dealWithBullet(@NotNull ImageObject bullet) {
		bullet.addAnim(new SimpleRotate(30));
		bullet.move(20, -20);
		bullet.addAnim(new AccurateMove((Math.random() - 0.5) * 800, (Math.random() - 0.5) * 800));
		int i = 0, duration = 40;
		for (; i < afterUsedBullet.size(); i++) {
			ImageResource afterBullet = afterUsedBullet.get(i);
			game.runLater(i * duration, () -> bullet.setRes(afterBullet));
		}
		game.runLater(i * duration, () -> bullet.setDied(true));
	}

	@Override
	public @NotNull ImageObject player() {
		return player;
	}
}
