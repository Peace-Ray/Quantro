package com.peaceray.quantro.content;

public class Skin {

	/**
	 * The game type used for the Skin.
	 * 
	 * @author Jake
	 * 
	 */
	public enum Game {
		/**
		 * Retro games have 1 qPane. Since blocks have no in-game depth, and are
		 * mostly homogenous in behavior, they can be drawn without opacity and
		 * with a greater range of meaningless colors.
		 */
		RETRO,

		/**
		 * Quantro games have 2 qPanes and several types of specialized pieces.
		 * Skins require some way of keeping one pane from obscuring the other,
		 * and some way of differentiating the different block types.
		 */
		QUANTRO
	}

	/**
	 * The template for the skin.
	 * 
	 * @author Jake
	 * 
	 */
	public enum Template {
		/**
		 * The standard template. Both Retro and Quantro support this type,
		 * which uses sharp, gradient-shined borders, solid-color inner fills,
		 * and Gaussian-blur shadows applied to the inside of blocks (apparently
		 * cast by the borders) and the outside of blocks (as a drop shadow).
		 * 
		 * Certain piece types have special draw procedures, such as flash
		 * blocks (use a custom bitmap for fill), Sticky pieces (use a 3D box
		 * draw between panes), gradient-fills based on local neighborhoods
		 * (used for SL blocks).
		 */
		STANDARD,

		/**
		 * The Colorblind template. Used only for Quantro; there is no need for
		 * colorblind help in Retro. Basically identical to Standard, except
		 * adds new visual details.
		 */
		COLORBLIND,

		/**
		 * Blocks are represented by a bright glowing border, shining both into
		 * and out of the block itself. Currently used only for Retro, and with
		 * only the NE0N color scheme.
		 */
		NEON
	}

	/**
	 * The color scheme applied to the template and game.
	 * 
	 * @author Jake
	 * 
	 */
	public enum Color {
		/**
		 * The standard retro scheme. Bright, striking colors.
		 */
		RETRO,

		/**
		 * The standard quantro scheme. Primarily red, blue and green, with a
		 * few others available for specialized blocks: orange and grey.
		 */
		QUANTRO,

		/**
		 * A bright, clean color scheme for Retro (maybe Quantro)? Colors match
		 * those of Standard Retro / Quantro, but they are only drawn for
		 * borders and color effects. Fills get a uniform light color, very
		 * close to white.
		 */
		CLEAN,

		/**
		 * Shades of red. Retro standard.
		 */
		RED,

		/**
		 * Shades of green. Retro standard.
		 */
		GREEN,

		/**
		 * Shades of blue. Retro standard.
		 */
		BLUE,

		/**
		 * Austere: plain white for almost everything.
		 */
		AUSTERE,

		/**
		 * Severe: Black, maybe with some white?
		 */
		SEVERE,

		/**
		 * A bold, primary-color color scheme.
		 */
		PRIMARY,

		/**
		 * A set of colors representing "bright shiny NE0N tubes."
		 */
		NEON,

		/**
		 * A striking, black-and-white color scheme with the basic NE0N visual
		 * style.
		 */
		LIMBO,

		/**
		 * Pale blues and bright golds. Used for Quantro.
		 */
		NILE,

		/**
		 * Quantro: natural wood colors. Somewhat difficult to distinguish s0
		 * and s1, but that's what natural colors get you...
		 */
		NATURAL,
		
		/**
		 * Quantro: colors based on zen garden.  Tan/grey and Green.
		 */
		ZEN,
		
		/**
		 * Quantro: colors of the dawn.
		 */
		DAWN,
		
		/**
		 * Quantro: colors for decadence.  Opulent riches.  S0 is bronze with
		 * a verdigris, while S1 is an extremely shiny gold.
		 */
		DECADENCE,

		/**
		 * Designed for maximum color-distinction for users with Protonopia
		 * (lack of red color receptors).
		 */
		PROTANOPIA,

		/**
		 * Designed for maximum color-distinction for users with Deuteranopia
		 * (lack of green color receptors).
		 */
		DEUTERANOPIA,

		/**
		 * Designed for maximum color-distinction for users with Tritanopia
		 * (lack of blue color receptors).
		 */
		TRITANOPIA
	}

	private Game mGame;
	private Template mTemplate;
	private Color mColor;

	private String mName;

	private Skin(Game g, Template t, Color c) {
		if (g == null || t == null || c == null)
			throw new NullPointerException("Must provide non-null values.");

		if (!Skin.isSkin(g, t, c))
			throw new IllegalArgumentException(
					"Invalid combination of game/template/color");

		mGame = g;
		mTemplate = t;
		mColor = c;

		mName = Skin.getName(mTemplate, mColor);
	}

	public Game getGame() {
		return mGame;
	}

	public Template getTemplate() {
		return mTemplate;
	}

	public Color getColor() {
		return mColor;
	}

	public String getName() {
		return mName;
	}

	public Color getColor(Game g) {
		if (mGame == g)
			return mColor;
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Skin))
			return false;

		Skin skin = (Skin) obj;
		if (mGame != skin.mGame)
			return false;
		if (mTemplate != skin.mTemplate)
			return false;
		if (mColor != skin.mColor)
			return false;

		return true;
	}

	public static boolean hasSkins(Game g, Template t) {
		if (g == null || t == null)
			return false;

		Color[] c_values = Color.values();
		for (int c = 0; c < c_values.length; c++) {
			if (isSkin(g, t, c_values[c]))
				return true;
		}

		return false;
	}

	/**
	 * Does the provided Game, Template, Color combination represent a Skin?
	 * 
	 * @param g
	 * @param t
	 * @param c
	 * @return
	 */
	public static boolean isSkin(Game g, Template t, Color c) {
		if (g == null || t == null || c == null)
			return false;

		switch (g) {
		case RETRO:
			switch (t) {
			case STANDARD:
				// supports retro, clean, and the three colorblindness colors.
				switch (c) {
				case RETRO:
				case CLEAN:
					// case RED: // put these back in when we have a good design
					// for them.
					// case GREEN:
					// case BLUE:
				case AUSTERE:
				case SEVERE:
				case PRIMARY:
				case PROTANOPIA:
				case DEUTERANOPIA:
				case TRITANOPIA:
					return true;
				}
				return false;

			case COLORBLIND:
				// no support; this is not a Retro template.
				return false;

			case NEON:
				switch (c) {
				case NEON:
				case LIMBO:
					return true;
				}
				return false;
			}

		case QUANTRO:
			switch (t) {
			case STANDARD:
				// supports quantro, nile, and the three colorblind schemes.
				switch (c) {
				case QUANTRO:
				case NILE:
				case NATURAL:
				case ZEN:
				case DAWN:
				case DECADENCE:
				case PROTANOPIA:
				case DEUTERANOPIA:
				case TRITANOPIA:
					return true;
				}
				return false;

			case COLORBLIND:
				// supports quantro, nile, and the three colorblind schemes.
				switch (c) {
				case QUANTRO:
				case NILE:
				case NATURAL:
				case ZEN:
				case DAWN:
				case DECADENCE:
				case PROTANOPIA:
				case DEUTERANOPIA:
				case TRITANOPIA:
					return true;
				}
				return false;

			case NEON:
				// no support; this is a Retro color scheme.
				return false;
			}
		}

		return false;
	}

	private static final Skin[][][] CACHED_SKINS = new Skin[Game.values().length][][];

	public static final Skin get(Game game, Template template, Color color) {
		if (!isSkin(game, template, color))
			return null;

		int g = game.ordinal();
		int t = template.ordinal();
		int c = color.ordinal();

		synchronized (CACHED_SKINS) {
			if (CACHED_SKINS[g] == null) {
				CACHED_SKINS[g] = new Skin[Template.values().length][];
			}
			Skin[][] tc_skins = CACHED_SKINS[g];

			if (tc_skins[t] == null) {
				tc_skins[t] = new Skin[Color.values().length];
			}
			Skin[] c_skins = tc_skins[t];

			if (c_skins[c] == null) {
				c_skins[c] = new Skin(game, template, color);
			}
			return c_skins[c];
		}
	}

	public static final Skin[] getAllSkins() {
		Game[] games = Game.values();
		Template[] templates = Template.values();
		Color[] colors = Color.values();

		int count = 0;
		for (int g = 0; g < games.length; g++) {
			for (int t = 0; t < templates.length; t++) {
				for (int c = 0; c < colors.length; c++) {
					if (isSkin(games[g], templates[t], colors[c])) {
						count++;
					}
				}
			}
		}

		Skin[] skins = new Skin[count];
		count = 0;
		for (int g = 0; g < games.length; g++) {
			for (int t = 0; t < templates.length; t++) {
				for (int c = 0; c < colors.length; c++) {
					if (isSkin(games[g], templates[t], colors[c])) {
						skins[count++] = get(games[g], templates[t], colors[c]);
					}
				}
			}
		}

		return skins;
	}

	private static final Skin[][] CACHED_SKINS_BY_GAME = new Skin[Game.values().length][];

	public static final Skin[] getSkins(Game game) {
		if (game == null)
			return null;

		int g = game.ordinal();

		synchronized (CACHED_SKINS_BY_GAME) {
			if (CACHED_SKINS_BY_GAME[g] == null) {
				// count them up.
				Template[] t_values = Template.values();
				Color[] c_values = Color.values();

				int count = 0;
				for (int t = 0; t < t_values.length; t++) {
					Template template = t_values[t];
					for (int c = 0; c < c_values.length; c++) {
						Color color = c_values[c];
						if (isSkin(game, template, color))
							count++;
					}
				}

				// populate the array.
				CACHED_SKINS_BY_GAME[g] = new Skin[count];
				count = 0;
				for (int t = 0; t < t_values.length; t++) {
					Template template = t_values[t];
					for (int c = 0; c < c_values.length; c++) {
						Color color = c_values[c];
						if (isSkin(game, template, color))
							CACHED_SKINS_BY_GAME[g][count++] = get(game,
									template, color);
					}
				}
			}

			return CACHED_SKINS_BY_GAME[g].clone();
		}
	}

	private static final Skin[][][] CACHED_SKINS_BY_GAME_TEMPLATE = new Skin[Game
			.values().length][Template.values().length][];

	public static final Skin[] getSkins(Game game, Template template) {
		if (game == null)
			return null;

		int g = game.ordinal();

		synchronized (CACHED_SKINS_BY_GAME_TEMPLATE) {
			int t = template.ordinal();

			if (CACHED_SKINS_BY_GAME_TEMPLATE[g][t] == null) {
				Color[] c_values = Color.values();

				int count = 0;
				for (int c = 0; c < c_values.length; c++) {
					Color color = c_values[c];
					if (isSkin(game, template, color))
						count++;
				}

				CACHED_SKINS_BY_GAME_TEMPLATE[g][t] = new Skin[count];
				count = 0;
				for (int c = 0; c < c_values.length; c++) {
					Color color = c_values[c];
					if (isSkin(game, template, color))
						CACHED_SKINS_BY_GAME_TEMPLATE[g][t][count++] = get(
								game, template, color);
				}
			}

			return CACHED_SKINS_BY_GAME_TEMPLATE[g][t].clone();
		}
	}

	private static final Template[][] TEMPLATES_BY_GAMES = new Template[Game
			.values().length][];

	public static final Template[] getTemplates(Game game) {
		int g = game.ordinal();
		if (TEMPLATES_BY_GAMES[g] == null) {
			int count = 0;
			Template[] templates = Template.values();
			for (int t = 0; t < templates.length; t++) {
				if (hasSkins(game, templates[t]))
					count++;
			}

			Template[] gameTemplates = new Template[count];
			count = 0;
			for (int t = 0; t < templates.length; t++) {
				if (hasSkins(game, templates[t]))
					gameTemplates[count++] = templates[t];
			}

			TEMPLATES_BY_GAMES[g] = gameTemplates;
		}

		return TEMPLATES_BY_GAMES[g];
	}

	public static final boolean equals(Skin s1, Skin s2) {
		if ((s1 == null) != (s2 == null))
			return false;
		if (s1 == null)
			return true;

		return s1.equals(s2);
	}

	public static final String getName(Template template) {
		switch (template) {
		case STANDARD:
			return "Standard";

		case COLORBLIND:
			return "Colorblind";

		case NEON:
			return "NE0N";

		}

		throw new IllegalArgumentException("Don't know the name of template "
				+ template);
	}

	public static final String getName(Color color) {
		switch (color) {
		case RETRO:
			return "Retro";
		case QUANTRO:
			return "Quantro";
		case CLEAN:
			return "Clean";
		case RED:
			return "Red";
		case GREEN:
			return "Green";
		case BLUE:
			return "Blue";
		case PRIMARY:
			return "Primary";
		case AUSTERE:
			return "Austere";
		case SEVERE:
			return "Severe";
		case NEON:
			return "NE0N";
		case LIMBO:
			return "Limbo";
		case NILE:
			return "Nile";
		case NATURAL:
			return "Natural" ;
		case ZEN:
			return "Zen" ;
		case DAWN:
			return "Dawn" ;
		case DECADENCE:
			return "Decadence" ;
		case PROTANOPIA:
			return "Protanopia";
		case DEUTERANOPIA:
			return "Deuteranopia";
		case TRITANOPIA:
			return "Tritanopia";
		}

		throw new IllegalArgumentException("Don't know the name of color "
				+ color);
	}

	public static final String getName(Template t, Color c) {
		String tName = getName(t);
		String cName = getName(c);

		if (tName.equals(cName))
			return tName;
		else
			return tName + " " + cName;
	}

	private static final String ENCODED_PREFIX = "S";
	private static final String ENCODED_OPEN = "(";
	private static final String ENCODED_CLOSE = ")";
	private static final String ENCODED_SEPARATOR = ",";

	private static final String[] ENCODED_GAME = new String[Game.values().length];
	private static final String[] ENCODED_TEMPLATE = new String[Template
			.values().length];
	private static final String[] ENCODED_COLOR = new String[Color.values().length];

	static {
		ENCODED_GAME[Game.QUANTRO.ordinal()] = "q";
		ENCODED_GAME[Game.RETRO.ordinal()] = "r";

		ENCODED_TEMPLATE[Template.STANDARD.ordinal()] = "s";
		ENCODED_TEMPLATE[Template.COLORBLIND.ordinal()] = "c";
		ENCODED_TEMPLATE[Template.NEON.ordinal()] = "n";

		ENCODED_COLOR[Color.QUANTRO.ordinal()] = "q";
		ENCODED_COLOR[Color.RETRO.ordinal()] = "r";
		ENCODED_COLOR[Color.CLEAN.ordinal()] = "c";
		ENCODED_COLOR[Color.RED.ordinal()] = "R";
		ENCODED_COLOR[Color.GREEN.ordinal()] = "G";
		ENCODED_COLOR[Color.BLUE.ordinal()] = "B";
		ENCODED_COLOR[Color.PRIMARY.ordinal()] = "P";
		ENCODED_COLOR[Color.AUSTERE.ordinal()] = "a";
		ENCODED_COLOR[Color.SEVERE.ordinal()] = "s";
		ENCODED_COLOR[Color.NEON.ordinal()] = "N";
		ENCODED_COLOR[Color.LIMBO.ordinal()] = "l";
		ENCODED_COLOR[Color.NILE.ordinal()] = "n";
		ENCODED_COLOR[Color.NATURAL.ordinal()] = "nat" ;
		ENCODED_COLOR[Color.ZEN.ordinal()] = "z" ;
		ENCODED_COLOR[Color.DAWN.ordinal()] = "dwn" ;
		ENCODED_COLOR[Color.DECADENCE.ordinal()] = "dec" ;
		ENCODED_COLOR[Color.PROTANOPIA.ordinal()] = "p";
		ENCODED_COLOR[Color.DEUTERANOPIA.ordinal()] = "d";
		ENCODED_COLOR[Color.TRITANOPIA.ordinal()] = "t";
	}

	/**
	 * Returns a "string encoded" version of this Skin, designed to be as
	 * lightweight as possible.
	 * 
	 * One possible use: storing, in SharedPreferences, a "StringSet" for the
	 * currently shuffled skins.
	 * 
	 * Encoding: uses the 26 alphabetical characters, in upper- and lower-case,
	 * parentheses "(" and ")", and the comma ",".
	 * 
	 * @param b
	 * @return
	 */
	public static final String toStringEncoding(Skin s) {
		return toStringEncoding(s.getGame(), s.getTemplate(), s.getColor());
	}

	/**
	 * Returns a "string encoded" version of this Skin, designed to be as
	 * lightweight as possible.
	 * 
	 * One possible use: storing, in SharedPreferences, a "StringSet" for the
	 * currently shuffled skins.
	 * 
	 * Encoding: uses the 26 alphabetical characters, in upper- and lower-case,
	 * parentheses "(" and ")", and the comma ",".
	 * 
	 * @param t
	 * @param c
	 * @return
	 */
	public static final String toStringEncoding(Game g, Template t, Color c) {
		String gStr = ENCODED_GAME[g.ordinal()]; // g-string
		String tStr = ENCODED_TEMPLATE[t.ordinal()];
		String cStr = ENCODED_COLOR[c.ordinal()];

		if (gStr == null || tStr == null || cStr == null)
			throw new IllegalArgumentException(
					"Don't have an encoding for Game " + g + " Template " + t
							+ " or Color " + c);

		StringBuilder sb = new StringBuilder();
		sb.append(ENCODED_PREFIX);
		sb.append(ENCODED_OPEN);
		sb.append(gStr);
		sb.append(ENCODED_SEPARATOR);
		sb.append(tStr);
		sb.append(ENCODED_SEPARATOR);
		sb.append(cStr);
		sb.append(ENCODED_CLOSE);

		return sb.toString();
	}

	/**
	 * Returns a Skin instance, representing the Skin object encoding in the
	 * provided string.
	 * 
	 * The provided string must be an encoding string as returned by
	 * toStringEncoding( ). For convenience, leading and trailing whitespace is
	 * allowed, at least that supported by 'trim()'. NO OTHER STRING CONTENT IS
	 * ALLOWED.
	 * 
	 * @param str
	 * @return
	 */
	public static final Skin fromStringEncoding(String str) {
		str = str.trim();

		int index_bg = str.indexOf(ENCODED_PREFIX);
		int index_op = str.indexOf(ENCODED_OPEN);
		int index_cm1 = str.indexOf(ENCODED_SEPARATOR);
		int index_cm2 = str.indexOf(ENCODED_SEPARATOR, index_cm1 + 1);
		int index_cp = str.indexOf(ENCODED_CLOSE);

		// verify
		if (index_bg == -1 || index_op == -1 || index_cm1 == -1
				|| index_cm2 == -1 || index_cp == -1)
			throw new IllegalArgumentException("Encoding " + str
					+ " does not match expected format.");
		if (index_bg > index_op || index_op > index_cm1
				|| index_cm1 > index_cm2 || index_cm2 > index_cp)
			throw new IllegalArgumentException("Encoding " + str
					+ " does not match expected format.");

		String gStr = str.substring(index_op + 1, index_cm1);
		String tStr = str.substring(index_cm1 + 1, index_cm2);
		String cStr = str.substring(index_cm2 + 1, index_cp);

		// get the template and shade
		int g = -1, t = -1, c = -1;

		for (int i = 0; i < ENCODED_GAME.length; i++) {
			if (gStr.equals(ENCODED_GAME[i]))
				g = i;
		}
		for (int i = 0; i < ENCODED_TEMPLATE.length; i++) {
			if (tStr.equals(ENCODED_TEMPLATE[i]))
				t = i;
		}
		for (int i = 0; i < ENCODED_COLOR.length; i++) {
			if (cStr.equals(ENCODED_COLOR[i]))
				c = i;
		}

		return Skin.get(Game.values()[g], Template.values()[t],
				Color.values()[c]);
	}

}
