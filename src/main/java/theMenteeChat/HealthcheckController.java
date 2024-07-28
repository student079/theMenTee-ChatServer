package theMenteeChat;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping
public class HealthcheckController {

    @GetMapping("/healthcheck")
    public String healthcheck() {
        return "OK";
    }
}
