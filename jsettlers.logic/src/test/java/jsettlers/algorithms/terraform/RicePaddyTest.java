/*
 * Copyright (c) 2026
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
 */
package jsettlers.algorithms.terraform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.movable.ESpellType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.player.MannaInformation;

public class RicePaddyTest {
	@Test
	public void testGrassBecomesWalkableGroundForRice() {
		Map<ShortPoint2D, ELandscapeType> landscape = new HashMap<>();
		ShortPoint2D first = new ShortPoint2D(1, 1);
		ShortPoint2D second = new ShortPoint2D(2, 1);
		landscape.put(first, ELandscapeType.GRASS);
		landscape.put(second, ELandscapeType.GRASS);

		new LandscapeEditor(landscape::get, landscape::put).fill(ELandscapeType.MOORBORDEROUTER, List.of(first, second));

		for (ELandscapeType type : landscape.values()) {
			assertEquals(ELandscapeType.MOORBORDEROUTER, type);
			assertTrue("rice is only planted on moor", type.isMoor());
			assertFalse("rice farmers have to reach it", type.isBlocking);
		}
		assertTrue(ELandscapeType.GRASS.isAllowedNeighbor(ELandscapeType.MOORBORDEROUTER));
	}

	@Test
	public void testSpellIsFreeAndOnlyForAsians() {
		MannaInformation asian = new MannaInformation(ECivilisation.ASIAN);

		assertEquals(0, asian.getSpellCost(ESpellType.CREATE_RICE_PADDY));
		assertTrue(asian.useSpell(ESpellType.CREATE_RICE_PADDY));
		assertTrue(asian.useSpell(ESpellType.CREATE_RICE_PADDY));
		assertEquals(0, asian.getSpellCost(ESpellType.CREATE_RICE_PADDY));
		assertEquals(0, asian.getAmountOfManna());

		assertFalse(new MannaInformation(ECivilisation.ROMAN).canUseSpell(ESpellType.CREATE_RICE_PADDY));
	}

	@Test
	public void testSavegameFromBeforeTheSpellCanCastIt() throws Exception {
		MannaInformation saved = new MannaInformation(ECivilisation.ASIAN);
		Field usedCount = MannaInformation.class.getDeclaredField("spellUsedCount");
		usedCount.setAccessible(true);
		short[] counts = (short[]) usedCount.get(saved);
		usedCount.set(saved, Arrays.copyOf(counts, ESpellType.CREATE_RICE_PADDY.ordinal()));

		assertTrue(serializeAndDeserialize(saved).useSpell(ESpellType.CREATE_RICE_PADDY));
	}

	private static MannaInformation serializeAndDeserialize(MannaInformation information) throws IOException, ClassNotFoundException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
			out.writeObject(information);
		}
		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			return (MannaInformation) in.readObject();
		}
	}
}
