package rusty.japaneseresolver;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record JishoEntry(List<JapaneseWord> japaneseWords, List<Sense> senses) {

	public String toString() {
		String wordString = japaneseWords.get(0).toString();
		String sensesString = senses.stream().map(s -> s.toString()).collect(Collectors.joining(" | "));


		return wordString + " ===> " + sensesString + " ■";
	}

	public record JapaneseWord(Optional<String> word, Optional<String> reading) {

		public String toString() {
			StringBuilder builder = new StringBuilder();
			if (word.isPresent()) {
				builder.append(word.get());
			}
			if (reading.isPresent()) {
				if (word.isPresent()) {
					builder.append(" (" + reading.get() + ")");
				} else {
					builder.append(reading.get());
				}
			}
			return builder.toString();
		}
	}

	public record Sense(List<String> meanings) {

		public String toString() {
			return meanings.stream().collect(Collectors.joining("; "));
		}

	}
}
