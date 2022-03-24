package eu.elg.heli;

import eu.elg.heli.impl.HeLI;
import eu.elg.heli.impl.HeLIResult;
import eu.elg.ltservice.ELGException;
import eu.elg.ltservice.LTService;
import eu.elg.model.AnnotationObject;
import eu.elg.model.Response;
import eu.elg.model.StandardMessages;
import eu.elg.model.StatusMessage;
import eu.elg.model.requests.TextRequest;
import eu.elg.model.responses.AnnotationsResponse;
import io.micronaut.http.annotation.Controller;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller("/process")
public class HeLIController extends LTService<TextRequest, LTService.Context> {

  private static final Pattern LINE_PATTERN = Pattern.compile("^.+$", Pattern.MULTILINE);

  @Override
  protected Response<?> handleSync(TextRequest request, Context ctx) throws Exception {
    boolean includeOrig = false;
    List<String> languages = null;
    if(request.getParams() != null) {
      if(request.getParams().containsKey("includeOrig") &&
              Boolean.parseBoolean(Objects.toString(request.getParams().get("includeOrig")))) {
        includeOrig = true;
      }
      if(request.getParams().containsKey("languageSet")) {
        Object langSet = request.getParams().get("languageSet");
        if(langSet instanceof List && ((List<?>) langSet).size() > 0 && ((List<?>) langSet).stream().allMatch((v) -> (v instanceof String))) {
          languages = (List<String>) langSet;
        } else {
          throw new ELGException(StandardMessages.elgServiceInternalError("\"languageSet\" parameter must be a list of strings"));
        }
        List<String> invalidLangs = new ArrayList<>(languages);
        invalidLangs.removeAll(HeLI.languageListFinal);
        if(invalidLangs.size() > 0) {
          throw new ELGException(new StatusMessage().withCode("heli.parameter.languageSet.partial.values.invalid").withText("\"languageSet\" parameter contains invalid languages: {0}").withParams(String.join(",", invalidLangs)));
        }
      }
    }

    // run language detection on each line of the input text
    int lineStart = 0;
    Matcher m = LINE_PATTERN.matcher(request.getContent());
    Map<String, List<AnnotationObject>> annotations = new HashMap<>();
    while(m.find()) {
      List<HeLIResult> result = HeLI.identifyLanguage(m.group(), languages);
      if(result.size() > 0) {
        AnnotationObject ann = new AnnotationObject().withOffsets(m.start(), m.end()).withFeatures("lang3", result.get(0).language3);
        if(result.get(0).language2 != null) {
          ann.withFeature("lang2", result.get(0).language2);
        }
        if(result.size() > 1) {
          // confidence is the difference in score between the first and second languages
          ann.withFeature("confidence", result.get(1).score - result.get(0).score);
        } else {
          ann.withFeature("confidence", 0.0f);
        }
        if(includeOrig) {
          ann.withFeature("original_text", m.group());
        }
        annotations.computeIfAbsent(result.get(0).language3, (l) -> new ArrayList<>()).add(ann);
      }
    }

    return new AnnotationsResponse().withAnnotations(annotations);
  }
}
