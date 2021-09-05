package ai.elimu.web.content.letter_sound_correspondence;

import java.util.List;
import org.apache.logging.log4j.Logger;
import ai.elimu.model.content.LetterSoundCorrespondence;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ai.elimu.dao.LetterSoundCorrespondenceDao;

@Controller
@RequestMapping("/content/letter-sound-correspondence/list")
public class LetterSoundCorrespondenceListController {
    
    private final Logger logger = LogManager.getLogger();
    
    @Autowired
    private LetterSoundCorrespondenceDao letterSoundCorrespondenceDao;

    @RequestMapping(method = RequestMethod.GET)
    public String handleRequest(Model model) {
    	logger.info("handleRequest");
        
        List<LetterSoundCorrespondence> letterSoundCorrespondences = letterSoundCorrespondenceDao.readAllOrderedByUsage();
        model.addAttribute("letterSoundCorrespondences", letterSoundCorrespondences);
        
        int maxUsageCount = 0;
        for (LetterSoundCorrespondence letterSoundCorrespondence : letterSoundCorrespondences) {
            if (letterSoundCorrespondence.getUsageCount() > maxUsageCount) {
                maxUsageCount = letterSoundCorrespondence.getUsageCount();
            }
        }
        model.addAttribute("maxUsageCount", maxUsageCount);

        return "content/letter-sound-correspondence/list";
    }
}
