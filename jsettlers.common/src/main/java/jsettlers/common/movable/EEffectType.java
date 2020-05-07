/*******************************************************************************
 * Copyright (c) 2019
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.common.movable;

import jsettlers.common.images.ImageLink;

public enum EEffectType {
	INCREASED_MORALE(60, 2f, "original_1_GUI_129"),
	DEFEATISM(60, 0.5f, "original_1_GUI_130"),
	GREEN_THUMB(600, 0.5f, "original_1_GUI_134");

	private int time;
	private float mod;
	private ImageLink link;

	EEffectType(int time, float mod, String imageLink) {
		this.time = time;
		this.mod = mod;
		link = ImageLink.fromName(imageLink);
	}

	public float getMod() {
		return mod;
	}

	public int getTime() {
		return time;
	}

	public ImageLink getImageLink() {
		return link;
	}
}
