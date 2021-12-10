package rusty.japaneseresolver;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record Kanji(String literal, List<String> meanings, Optional<List<String>> onyomi, Optional<List<String>> kunyomi) {
	
	public String toString() {

		String meaningsString = meanings.stream().collect(Collectors.joining("; "));

		StringBuilder builder = new StringBuilder();

		if (onyomi.isPresent()) {
			builder.append(" <| ");

			String onyomiList = onyomi.get().stream().collect(Collectors.joining(" | "));
			builder.append(onyomiList);

			builder.append(" |>");
		}

		if (kunyomi.isPresent()) {
			builder.append(" <| ");

			String kunyomiList = kunyomi.get().stream().collect(Collectors.joining(" | "));
			builder.append(kunyomiList);

			builder.append(" |>");
		}

		String readings = builder.toString();

		return literal + readings + " ===> " + meaningsString + " ■";
	}
}
