package org.frice.th.self;

import org.frice.anim.move.AccurateMove;
import org.frice.obj.sub.ImageObject;
import org.frice.resource.image.ImageResource;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static java.lang.Math.PI;

public interface GensokyoManager {
	@NotNull ImageObject player();

	@NotNull List<@NotNull ImageObject> bullets();

	void dealWithBullet(@NotNull ImageObject bullet);

	class DefaultImpl {
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
	}
}
