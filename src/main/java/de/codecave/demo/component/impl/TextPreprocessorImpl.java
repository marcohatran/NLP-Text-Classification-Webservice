package de.codecave.demo.component.impl;

import com.google.common.base.Preconditions;
import de.codecave.demo.component.*;
import de.codecave.demo.util.Python3Compat;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Functions;
import org.apache.commons.lang3.StringUtils;
import org.deeplearning4j.nn.modelimport.keras.preprocessing.text.KerasTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class TextPreprocessorImpl implements TextPreprocessor {

    @Autowired
    private LemmatizerService lemmatizerService;

    @Autowired
    private TokenizerService tokenizerService;

    @Autowired
    private PaddingService paddingService;

    private static String removePunctuation(String text) {
        final String punctuations = Pattern.quote("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~");
        return text.replaceAll("[" + punctuations + "]", "");
    }

    private Set<String> stopWords;

    public TextPreprocessorImpl() {
        this.stopWords = loadStopWordsFromTxtFile();
    }

    public void setLemmatizerService(LemmatizerService lemmatizerService) {
        this.lemmatizerService = lemmatizerService;
    }

    public void setTokenizerService(TokenizerService tokenizerService) {
        this.tokenizerService = tokenizerService;
    }

    public void setPaddingService(PaddingService paddingService) {
        this.paddingService = paddingService;
    }

    @Override
    public String cleanText(String text) {
        /** Implementierung der Textbereinigung in Java
         * Stopwords sind "oben" zu finden, die kann man evtl. über eine File reinladen die ich aus Python exportiere
         * Die Werte von string.punctuation kann ich ebenfalls liefern
         * Der Stemmer wird aktuell nicht verwendet, kann daher entfallen
         * Für den WordNetLemmatizer muss eine Java Implementierung gefunden werden
         *
         * Das hier könnte als Lemmatizer funktionieren https://stanfordnlp.github.io/CoreNLP/simple.html
         *
         *
         *
         */

        final Pattern REGEX_WHITESPACE = Pattern.compile("\\s");

        return
                REGEX_WHITESPACE.splitAsStream(text)
                        .map(tok -> lemmatizerService.tryLemmatize(tok).orElse(tok))
                        .map(tok -> tok.toLowerCase(Locale.ENGLISH))
                        .map(String::trim) // python3 string.strip The strip() method removes any whitespace from the beginning or the end:
                        .map(tok -> removePunctuation(tok))
                        .map(tok -> Python3Compat.isnumeric(tok) ? "<num>" : tok)
                        .filter(tok -> !stopWords.contains(tok))
                        .collect(Collectors.joining(" "));
    }

/*
def clean_text(x, stemming=False, lemmatization=True):
    """
    clean text x
    :param x: List of sentences/strings to be tokenized
    :return: Tuple of cleaned data without stopwords, to lower, etc.
    """
    def remove_punctuation(word):
        word = ''.join([char for char in word if char not in string.punctuation])
        return word

    stemmer = PorterStemmer()
    lemmatizer = WordNetLemmatizer()

    cleaned = []
    for text in x:
        tokens = text.split()
        tokens = [tok.lower().strip() for tok in tokens]

        # Remove punctuation
        tokens = [remove_punctuation(tok) for tok in tokens]

        # Remove numbers
        tokens = ['#num#' if tok.isnumeric() else tok for tok in tokens]

        # Remove Stopwords
        tokens = [tok for tok in tokens if tok not in stop_words and tok]

        # Stemming
        if stemming:
            tokens = [stemmer.stem(tok) for tok in tokens]

        if lemmatization:
            tokens = [lemmatizer.lemmatize(tok) for tok in tokens]

        tokens = ' '.join(tokens)
        cleaned.append(tokens)

    return cleaned
  */

    @Override
    public int[] tokenize(String text) {
        return tokenizerService.textToSequence(text);
    }

    @Override
    public int[] padding(int[] tokenizedText) {
        return paddingService.padding(tokenizedText);
    }

    @Override
    public int[] pipeline(String text) {
        return this.padding(this.tokenize(this.cleanText(text)));
    }

    private Set<String> loadStopWordsFromTxtFile() {
        final InputStream is = TextPreprocessor.class.getResourceAsStream("/nlp/stopwords.txt");
        Preconditions.checkNotNull("stopwords file not found");
        return
            new BufferedReader(new InputStreamReader(
                    is)).lines()
                .peek(word -> Preconditions.checkState(StringUtils.isNotBlank(word)))
                .collect(Collectors.toSet());
    }

}
