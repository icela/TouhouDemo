package org.frice.th;

import org.frice.anim.move.AccurateMove;
import org.frice.obj.sub.ImageObject;
import org.frice.resource.image.FrameImageResource;
import org.frice.resource.image.ImageResource;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.PI;

public interface SelfPlane {
	@NotNull ImageObject player();

	@NotNull List<@NotNull ImageObject> bullets();

	@NotNull
	static List<@NotNull ImageObject> makeLeftRightBullets(@NotNull ImageObject player, @NotNull ImageResource bullet) {
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

	class Reimu implements SelfPlane {
		@NotNull Touhou game;
		@NotNull
		static String REIMU_RES = "./res/th11/player/pl00/pl00.png";
		@NotNull
		static ImageResource bigImage = ImageResource.fromPath(REIMU_RES);
		@NotNull ImageResource mainBullet = bigImage.part(0, 176, 64, 16);
		@NotNull
		private ImageObject player;

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

		@NotNull
		@Override
		public ImageObject player() {
			return player;
		}
	}

	class Marisa implements SelfPlane {
		@NotNull
		static String MARISA_RES = "./res/th11/player/pl01/pl01.png";
		@NotNull
		static ImageResource bigImage = ImageResource.fromPath(MARISA_RES);
		@NotNull ImageResource mainBullet = bigImage.part(0, 144, 32, 16);
		@NotNull Touhou game;
		@NotNull ImageObject player;

		Marisa(@NotNull Touhou game) {
			this.game = game;
			player = new ImageObject(new FrameImageResource(IntStream.range(0, 8)
					.mapToObj(x -> bigImage.part(x * 32, 0, 32, 48))
					.collect(Collectors.toList()), 50), (Touhou.sceneWidth >>> 1) - 1, game.getHeight() - 50);
		}

		@NotNull
		@Override
		public ImageObject player() {
			return player;
		}

		@Override
		public @NotNull List<@NotNull ImageObject> bullets() {
			return makeLeftRightBullets(player, mainBullet);
		}
	}
}
