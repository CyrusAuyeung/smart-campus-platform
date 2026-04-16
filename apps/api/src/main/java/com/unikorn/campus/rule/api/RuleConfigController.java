package com.unikorn.campus.rule.api;

import com.unikorn.campus.rule.engine.RuleConfigRepository;
import com.unikorn.campus.rule.engine.RuleConfigView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/rules")
public class RuleConfigController {

    private final RuleConfigRepository ruleConfigRepository;

    public RuleConfigController(RuleConfigRepository ruleConfigRepository) {
        this.ruleConfigRepository = ruleConfigRepository;
    }

    @GetMapping
    public List<RuleConfigView> listRuleConfigs() {
        return ruleConfigRepository.findAllRuleConfigs();
    }
}