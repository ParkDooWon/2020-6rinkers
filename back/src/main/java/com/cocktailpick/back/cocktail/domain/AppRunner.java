package com.cocktailpick.back.cocktail.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.cocktailpick.back.cocktail.service.CocktailService;

@Component
public class AppRunner implements CommandLineRunner {
	private static final Logger logger = LoggerFactory.getLogger(AppRunner.class);

	private final CocktailService cocktailService;

	public AppRunner(CocktailService cocktailService) {
		this.cocktailService = cocktailService;
	}

	@Override
	public void run(String... args) throws Exception {
		logger.info("START!!");        // 메서드를 시작합니다.

		logger.info("FIND " + cocktailService.findTemp(1L).getNum());
		logger.info("FIND " + cocktailService.findTemp(2L).getNum());

		logger.info("FIND " + cocktailService.findTemp(1L).getNum());
		logger.info("FIND " + cocktailService.findTemp(2L).getNum());

		logger.info("UPDATE id 1 -> 4L " + cocktailService.updateTemp(1L, 4L));
		logger.info("UPDATE id 2 -> 5L " + cocktailService.updateTemp(2L, 5L));

		logger.info("FIND " + cocktailService.findTemp(1L).getNum());
		logger.info("FIND " + cocktailService.findTemp(2L).getNum());
	}
}
