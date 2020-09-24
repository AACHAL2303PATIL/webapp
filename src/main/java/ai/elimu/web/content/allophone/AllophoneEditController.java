package ai.elimu.web.content.allophone;

import java.util.Calendar;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.apache.commons.lang.StringUtils;

import org.apache.logging.log4j.Logger;
import ai.elimu.dao.AllophoneDao;
import ai.elimu.model.content.Allophone;
import ai.elimu.model.enums.content.allophone.SoundType;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/content/allophone/edit")
public class AllophoneEditController {
    
    private final Logger logger = LogManager.getLogger();
    
    @Autowired
    private AllophoneDao allophoneDao;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String handleRequest(Model model, @PathVariable Long id) {
    	logger.info("handleRequest");
        
        Allophone allophone = allophoneDao.read(id);
        model.addAttribute("allophone", allophone);
        
        model.addAttribute("soundTypes", SoundType.values());

        return "content/allophone/edit";
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    public String handleSubmit(
            @PathVariable Long id,
            @Valid Allophone allophone,
            BindingResult result,
            Model model,
            HttpSession session
    ) {
    	logger.info("handleSubmit");
        
        if (StringUtils.isNotBlank(allophone.getValueIpa())) {
            Allophone existingAllophone = allophoneDao.readByValueIpa(allophone.getValueIpa());
            if ((existingAllophone != null) && !existingAllophone.getId().equals(allophone.getId())) {
                result.rejectValue("valueIpa", "NonUnique");
            }
        }
        
        if (StringUtils.isNotBlank(allophone.getValueSampa())) {
            Allophone existingAllophone = allophoneDao.readByValueSampa(allophone.getValueSampa());
            if ((existingAllophone != null) && !existingAllophone.getId().equals(allophone.getId())) {
                result.rejectValue("valueSampa", "NonUnique");
            }
        }
        
        if (result.hasErrors()) {
            model.addAttribute("allophone", allophone);
            model.addAttribute("soundTypes", SoundType.values());
            return "content/allophone/edit";
        } else {
            allophone.setTimeLastUpdate(Calendar.getInstance());
            allophone.setRevisionNumber(allophone.getRevisionNumber() + 1);
            allophoneDao.update(allophone);            
            return "redirect:/content/allophone/list#" + allophone.getId();
        }
    }
}
