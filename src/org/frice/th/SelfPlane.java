package org.frice.th;

import org.frice.obj.sub.ImageObject;
import org.frice.resource.image.FrameImageResource;
import org.frice.resource.image.ImageResource;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface SelfPlane {
	String MARISA_RES = "./res/th11/player/pl01/pl01.png";
	String REIMU_RES = "./res/th11/player/pl00/pl00.png";

	@NotNull ImageObject player();

	abstract class Reimu implements SelfPlane {
		Touhou game;

		Reimu(Touhou game) {
			this.game = game;
		}

	}

	class ReimuA extends Reimu {

		ReimuA(Touhou game) {
			super(game);
		}

		@NotNull
		@Override
		public ImageObject player() {
			ImageResource bigImage = ImageResource.fromPath(REIMU_RES);
			return new ImageObject(new FrameImageResource(IntStream.range(0, 8)
					.mapToObj(x -> bigImage.part(x * 32, 0, 32, 48))
					.collect(Collectors.toList()), 50), (Touhou.sceneWidth >>> 1) - 1, game.getHeight() - 50);
		}
	}
}
