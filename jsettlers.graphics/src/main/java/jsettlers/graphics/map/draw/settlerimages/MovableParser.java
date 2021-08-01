package jsettlers.graphics.map.draw.settlerimages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MovableParser {

	private final Map<String, MovableTemplate> templates = new HashMap<>();

	private final OpenResourceFunction openResource;

	public MovableParser(OpenResourceFunction openResource) {
		this.openResource = openResource;
	}

	public interface OpenResourceFunction {
		Reader openResource(String name) throws IOException;
	}

	private class MovableTemplate implements Consumer<String> {
		private final List<String> arguments = new ArrayList<>();
		private final List<String> lines = new ArrayList<>();

		private MovableTemplate(String declaration) {
			int argBegin = declaration.indexOf(' ');
			String name = declaration.substring(1, argBegin);
			templates.put(name, this);

			int lastSpace = argBegin;
			while(declaration.charAt(lastSpace+1) != '{') {
				int nextSpace = declaration.indexOf(' ', lastSpace+1);
				arguments.add("$" + declaration.substring(lastSpace+1, nextSpace));
				lastSpace = nextSpace;
			}
		}


		@Override
		public void accept(String line) {
			lines.add(line);
		}
	}

	private void invokeTemplate(String line, Consumer<String> lineCons) {
		int argBegin = line.indexOf('(');
		String name = line.substring(0, argBegin);
		MovableTemplate template = templates.get(name);

		if(template == null) {
			System.err.println("template " + name + " is undefined!");
			return;
		} else if(template == lineCons) {
			System.err.println("template " + name + " is recursive!");
			return;
		}

		HashMap<String, String> variableReplace = new HashMap<>(template.arguments.size());

		int lastComma = argBegin;
		int index = 0;
		while(line.charAt(lastComma) != ')') {
			int nextComma = line.indexOf(',', lastComma+1);
			if(nextComma == -1) nextComma = line.indexOf(')');

			String arg = template.arguments.get(index);
			variableReplace.put(arg, Matcher.quoteReplacement(line.substring(lastComma+1, nextComma)));
			index++;

			lastComma = nextComma;
		}

		for(String templateLine : template.lines) {

			for(Map.Entry<String, String> variable : variableReplace.entrySet()) {
				templateLine = Pattern.compile(variable.getKey(), Pattern.LITERAL).matcher(templateLine).replaceAll(variable.getValue());
			}

			lineCons.accept(templateLine);
		}
	}

	public void parseFile(String resource, Consumer<String> external) throws IOException {
		parseFile(openResource.openResource(resource), external);
	}

	private void parseFile(Reader input, Consumer<String> external) throws IOException {
		try(BufferedReader reader = new BufferedReader(input)) {
			Consumer<String> lineConsumer = external;

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) continue;
				if (line.startsWith("#")) continue;

				if (line.startsWith("}")) {
					if (lineConsumer == external) {
						System.err.println("} is out of place!");
					}
					lineConsumer = external;
					continue;
				}

				if (line.startsWith("!")) {
					lineConsumer = new MovableTemplate(line);
				} else if (line.contains("(")) {
					invokeTemplate(line, lineConsumer);
				} else {
					lineConsumer.accept(line);
				}
			}
		}
	}
}
