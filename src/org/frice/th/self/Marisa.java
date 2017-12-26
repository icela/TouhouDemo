package org.frice.th.self;

import org.frice.obj.sub.ImageObject;
import org.frice.resource.image.FrameImageResource;
import org.frice.resource.image.ImageResource;
import org.frice.th.Touhou;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.frice.th.self.GensokyoManager.DefaultImpl.makeLeftRightBullets;

public class Marisa implements GensokyoManager {
	private static @NotNull String MARISA_RES = Touhou.sourceRoot + "/th11/player/pl01/pl01.png";
	private static @NotNull ImageResource bigImage = ImageResource.fromPath(MARISA_RES);
	private @NotNull ImageResource mainBullet = bigImage.part(0, 144, 32, 16);
	private @NotNull Touhou game;
	private @NotNull ImageObject player;

	private @NotNull List<@NotNull ImageResource> afterUsedBullet = IntStream.range(1, 4)
			.mapToObj(i -> bigImage.part(i * 16, 144, 32, 16))
			.collect(Collectors.toList());

	public Marisa(@NotNull Touhou game) {
		this.game = game;
		player = new ImageObject(new FrameImageResource(IntStream.range(0, 8)
				.mapToObj(x -> bigImage.part(x * 32, 0, 32, 48))
				.collect(Collectors.toList()), 50), (Touhou.stageWidth >>> 1) - 1, game.getHeight() - 50);
	}

	@Override
	public @NotNull ImageObject player() {
		return player;
	}

	@Override
	public @NotNull List<@NotNull ImageObject> bullets() {
		return makeLeftRightBullets(player, mainBullet);
	}

	@Override
	public void dealWithBullet(@NotNull ImageObject bullet) {
		int i = 0, duration = 30;
		for (; i < afterUsedBullet.size(); ) {
			ImageResource afterBullet = afterUsedBullet.get(i++);
			game.runLater(i * duration, () -> bullet.setRes(afterBullet));
		}
		game.runLater(i * duration + 10, () -> bullet.setDied(true));
	}
}


































