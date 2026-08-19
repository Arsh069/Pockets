package com.marsh.pockets.pocket.scheduler;

import com.marsh.pockets.auth.entity.User;
import com.marsh.pockets.auth.repository.UserRepository;
import com.marsh.pockets.common.util.PayDayUtil;
import com.marsh.pockets.pocket.entity.Pocket;
import com.marsh.pockets.pocket.repository.PocketRepository;
import com.marsh.pockets.pocket.service.PocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class PocketResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(PocketResetScheduler.class);

    private final UserRepository userRepository;
    private final PocketRepository pocketRepository;
    private final PocketService pocketService;

    public PocketResetScheduler(
            UserRepository userRepository,
            PocketRepository pocketRepository,
            PocketService pocketService) {
        this.userRepository = userRepository;
        this.pocketRepository = pocketRepository;
        this.pocketService = pocketService;
    }

    @Scheduled(cron = "${pocket.reset.cron:0 0 2 * * *}")
    @Transactional
    public void processMonthlyResets() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        List<User> allUsers = userRepository.findAll();

        int usersProcessed = 0;
        int pocketsReset = 0;
        int pocketsSkipped = 0;

        for (User user : allUsers) {
            if (user.getPayDay() != null && PayDayUtil.isResetDay(user.getPayDay(), today)) {
                usersProcessed++;
                List<Pocket> pockets = pocketRepository.findByUserId(user.getId());

                for (Pocket pocket : pockets) {
                    boolean alreadyResetToday = false;
                    if (pocket.getLastResetAt() != null) {
                        LocalDate lastResetDate = LocalDate.ofInstant(pocket.getLastResetAt(), ZoneId.systemDefault());
                        if (lastResetDate.equals(today)) {
                            alreadyResetToday = true;
                        }
                    }

                    if (alreadyResetToday) {
                        pocketsSkipped++;
                    } else {
                        // Scheduled reset IGNORES manual override protection (ignoreOverrideProtection = true)
                        pocketService.resetBalance(pocket.getId(), true);
                        pocketsReset++;
                    }
                }
            }
        }

        log.info("Monthly reset job summary: Users processed = {}, Pockets reset = {}, Pockets skipped = {}",
                usersProcessed, pocketsReset, pocketsSkipped);
    }
}
