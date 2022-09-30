package eu.elg.heli;

import eu.elg.heli.impl.HeLI;
import eu.elg.heli.impl.HeLIResult;
import eu.elg.ltservice.ELGException;
import eu.elg.ltservice.LTService;
import eu.elg.model.Response;
import eu.elg.model.StandardMessages;
import eu.elg.model.StatusMessage;
import eu.elg.model.requests.TextRequest;
import eu.elg.model.responses.ClassificationClass;
import eu.elg.model.responses.ClassificationResponse;
import io.micronaut.http.annotation.Controller;

import java.util.*;
import java.io.StringWriter;
import java.io.PrintWriter;

@Controller("/process")
public class HeLIController extends LTService<TextRequest, LTService.Context> {

  @Override
  protected Response<?> handleSync(TextRequest request, Context ctx) throws Exception {
    int nbest = 5;
    List<String> languages = null;
    if(request.getParams() != null) {
      if(request.getParams().containsKey("nbest")) {
        String nstr = "";
        try {
            nstr = Objects.toString(request.getParams().get("nbest"));
            int nb = Integer.parseInt(nstr);
            nbest = nb;
        } catch (Exception e) {
          throw new ELGException(new StatusMessage().withCode("heli.parameter.invalid").withText("can not parse \"nbest\" to integer: {0}").withParams(nstr));
        }
      }
      if(request.getParams().containsKey("languages")) {
        Object langSet = request.getParams().get("languages");
        if(langSet instanceof List && ((List<?>) langSet).size() > 0 && ((List<?>) langSet).stream().allMatch(v -> v instanceof String)) {
          languages = (List<String>) langSet;
        } else {
          throw new ELGException(StandardMessages.elgServiceInternalError("\"languages\" parameter must be a list of strings"));
        }
        List<String> invalidLangs = new ArrayList<>(languages);
        invalidLangs.removeAll(HeLI.languageListFinal);
        if(invalidLangs.size() > 0) {
          throw new ELGException(new StatusMessage().withCode("heli.parameter.invalid").withText("\"languages\" parameter contains invalid languages: {0}").withParams(String.join(",", invalidLangs)));
        }
      }
    }

    List<ClassificationClass> classes = new ArrayList<>();
    try {
        List<HeLIResult> results = HeLI.identifyLanguage(request.getContent(), languages, nbest);
        for (HeLIResult res : results) {
            ClassificationClass cl = new ClassificationClass();
            cl.withClassName(res.language).withScore(res.score);
            classes.add(cl);
        }
    } catch (Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        throw new ELGException(new StatusMessage().withCode("heli.internal.error").withText("Something went wrong: {0}").withParams(exceptionAsString));
    }
    return new ClassificationResponse().withClasses(classes);
  }
}
