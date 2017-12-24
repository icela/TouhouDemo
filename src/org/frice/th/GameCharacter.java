package org.frice.th;

import org.frice.anim.move.AccurateMove;
import org.frice.anim.rotate.SimpleRotate;
import org.frice.obj.sub.ImageObject;
import org.frice.resource.image.FrameImageResource;
import org.frice.resource.image.ImageResource;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.PI;

public interface GameCharacter {
	@NotNull ImageObject player();

	@NotNull List<@NotNull ImageObject> bullets();

	static @NotNull List<@NotNull ImageObject> makeLeftRightBullets(
			@NotNull ImageObject player, @NotNull ImageResource bullet) {
		ImageObject left = new ImageObject(bullet,
				player.getX() + (player.getWidth() - bullet.getImage().getWidth()) / 2 - 10,
				player.getY());
		ImageObject right = new ImageObject(bullet,
				player.getX() + (player.getWidth() - bullet.getImage().getWidth()) / 2 + 10,
				player.getY());
		left.rotate(-PI / 2);
		right.rotate(-PI / 2);
		left.addAnim(new AccurateMove(0, -1000));
		right.addAnim(new AccurateMove(0, -1000));
		return Arrays.asList(left, right);
	}

	void dealWithBullet(@NotNull ImageObject bullet);

	class Reimu implements GameCharacter {
		@NotNull Touhou game;
		@NotNull String REIMU_RES = Touhou.sourceRoot + "/th11/player/pl00/pl00.png";
		@NotNull ImageResource bigImage = ImageResource.fromPath(REIMU_RES);
		private @NotNull ImageResource mainBullet = bigImage.part(0, 176, 64, 16);
		private @NotNull List<@NotNull ImageResource> afterUsedBullet = IntStream.of(0, 1, 3)
				.mapToObj(i -> bigImage.part(i * 8, 144, 16, 16))
				.collect(Collectors.toList());
		private @NotNull ImageObject player;

		Reimu(@NotNull Touhou game) {
			this.game = game;
			player = new ImageObject(new FrameImageResource(IntStream.range(0, 8)
					.mapToObj(x -> bigImage.part(x * 32, 0, 32, 48))
					.collect(Collectors.toList()), 50), (Touhou.sceneWidth >>> 1) - 1, game.getHeight() - 50);
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

	class Marisa implements GameCharacter {
		static @NotNull String MARISA_RES = Touhou.sourceRoot + "/th11/player/pl01/pl01.png";
		static @NotNull ImageResource bigImage = ImageResource.fromPath(MARISA_RES);
		@NotNull ImageResource mainBullet = bigImage.part(0, 144, 32, 16);
		@NotNull Touhou game;
		@NotNull ImageObject player;
		private @NotNull List<@NotNull ImageResource> afterUsedBullet = IntStream.range(1, 4)
				.mapToObj(i -> bigImage.part(i * 16, 144, 32, 16))
				.collect(Collectors.toList());

		Marisa(@NotNull Touhou game) {
			this.game = game;
			player = new ImageObject(new FrameImageResource(IntStream.range(0, 8)
					.mapToObj(x -> bigImage.part(x * 32, 0, 32, 48))
					.collect(Collectors.toList()), 50), (Touhou.sceneWidth >>> 1) - 1, game.getHeight() - 50);
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
}
