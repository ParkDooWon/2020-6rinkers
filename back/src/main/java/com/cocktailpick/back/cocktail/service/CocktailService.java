package com.cocktailpick.back.cocktail.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cocktailpick.back.cocktail.domain.Cocktail;
import com.cocktailpick.back.cocktail.domain.CocktailFindStrategyFactory;
import com.cocktailpick.back.cocktail.domain.CocktailRepository;
import com.cocktailpick.back.cocktail.domain.CocktailSearcher;
import com.cocktailpick.back.cocktail.domain.Temp;
import com.cocktailpick.back.cocktail.dto.CocktailDetailResponse;
import com.cocktailpick.back.cocktail.dto.CocktailRequest;
import com.cocktailpick.back.cocktail.dto.CocktailResponse;
import com.cocktailpick.back.common.EntityMapper;
import com.cocktailpick.back.common.csv.OpenCsvReader;
import com.cocktailpick.back.common.domain.DailyDate;
import com.cocktailpick.back.common.exceptions.EntityNotFoundException;
import com.cocktailpick.back.common.exceptions.ErrorCode;
import com.cocktailpick.back.common.util.NumberOfDaily;
import com.cocktailpick.back.recipe.domain.RecipeItem;
import com.cocktailpick.back.tag.domain.CocktailTag;
import com.cocktailpick.back.tag.domain.Tag;
import com.cocktailpick.back.tag.domain.TagRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
@Service
public class CocktailService {
	private final CocktailRepository cocktailRepository;
	private final TagRepository tagRepository;
	private final CocktailFindStrategyFactory cocktailFindStrategyFactory;

	@Cacheable("allCocktails")
	@Transactional(readOnly = true)
	public List<CocktailResponse> findAllCocktails() throws InterruptedException {
		System.err.println("모든 칵테일을 조회합니다!");
		Thread.sleep(3 * 1000);

		return new ArrayList<>();
	}

	@Cacheable(value = "cocktails", key = "#contain + #id + #size")
	@Transactional(readOnly = true)
	public List<CocktailResponse> findPageContainingWord(String contain, long id, int size) throws
		InterruptedException {
		System.err.println("특정 단어가 포함된 칵테일 찾기를 시작합니다!");
		Thread.sleep(3 * 1000);

		return new ArrayList<>();
	}

	@Cacheable(value = "nameCocktails", key = "#tagIds + #id + #size")
	@Transactional(readOnly = true)
	public List<CocktailResponse> findPageFilteredByTags(List<Long> tagIds, long id, int size) throws
		InterruptedException {
		Thread.sleep(3 * 1000);
		try {
			List<Cocktail> persistCocktails = cocktailRepository.findByIdGreaterThan(id);

			List<Cocktail> cocktails = persistCocktails.stream()
				.filter(cocktail -> cocktail.containTagIds(tagIds))
				.limit(size)
				.collect(Collectors.toList());
			return CocktailResponse.listOf(cocktails);
		} catch (Exception e) {
			System.err.println("");
		}

		return null;
	}

	@Cacheable(value = "cocktail", key = "#id")
	@Transactional(readOnly = true)
	public CocktailDetailResponse findCocktail(Long id) throws InterruptedException {
		System.err.println("칵테일 찾기를 시작합니다!");
		Thread.sleep(3 * 1000);
		Cocktail cocktail = findById(id);
		return CocktailDetailResponse.of(cocktail);
	}

	@Transactional
	public Long save(CocktailRequest cocktailRequest) {
		Cocktail cocktail = cocktailRequest.toCocktail();
		cocktailRepository.save(cocktail);

		List<RecipeItem> recipeItems = cocktailRequest.toRecipeItems();
		setCocktail(cocktail, recipeItems);

		List<Tag> tags = tagRepository.findByNameIn(cocktailRequest.getTag());
		associate(cocktail, tags);

		return cocktail.getId();
	}

	@CachePut(value = "cocktail", key = "#id")
	@Transactional
	public void updateCocktail(Long id, CocktailRequest cocktailRequest) {
		Cocktail cocktail = findById(id);
		Cocktail requestCocktail = cocktailRequest.toCocktail();
		List<RecipeItem> recipeItems = cocktailRequest.toRecipeItems();
		List<Tag> tags = tagRepository.findByNameIn(cocktailRequest.getTag());

		cocktail.update(requestCocktail, tags, recipeItems);
	}

	private Cocktail findById(Long id) {
		return cocktailRepository.findById(id)
			.orElseThrow(() -> new EntityNotFoundException(ErrorCode.COCKTAIL_NOT_FOUND));
	}

	// @CacheEvict(value = "cocktail", key = "#id")
	@Transactional
	public void deleteCocktail(Long id) {
		cocktailRepository.deleteById(id);
	}

	@CacheEvict(value = "allCocktails")
	@Transactional
	public void deleteAllCocktails() {
		System.err.println("모든 칵테일을 삭제했습니다.");
	}

	@Transactional
	public void saveAll(MultipartFile file) {
		CocktailCsvReader cocktailCsvReader = createCsvReader(file);
		List<CocktailRequest> cocktailRequests = cocktailCsvReader.getCocktailRequests();

		List<Tag> allTags = tagRepository.findAll();
		EntityMapper<String, Tag> tagMapper = mapTagToName(allTags);

		List<Cocktail> cocktails = new ArrayList<>();
		for (CocktailRequest cocktailRequest : cocktailRequests) {
			Cocktail cocktail = cocktailRequest.toCocktail();

			List<RecipeItem> recipeItems = cocktailRequest.toRecipeItems();
			setCocktail(cocktail, recipeItems);

			List<String> tagNames = cocktailRequest.getTag();
			List<Tag> tags = getTagsByName(tagMapper, tagNames);

			associate(cocktail, tags);
			cocktails.add(cocktail);
		}
		cocktailRepository.saveAll(cocktails);
	}

	private EntityMapper<String, Tag> mapTagToName(List<Tag> allTags) {
		EntityMapper<String, Tag> tagMapper = new EntityMapper<>(new HashMap<>());
		for (Tag tag : allTags) {
			tagMapper.put(tag.getName(), tag);
		}
		return tagMapper;
	}

	private CocktailCsvReader createCsvReader(MultipartFile file) {
		return new CocktailCsvReader(OpenCsvReader.from(file));
	}

	private void setCocktail(Cocktail cocktail, List<RecipeItem> recipeItems) {
		for (RecipeItem recipeItem : recipeItems) {
			recipeItem.setCocktail(cocktail);
		}
	}

	private List<Tag> getTagsByName(EntityMapper<String, Tag> tagMapper, List<String> tagNames) {
		return tagNames.stream()
			.map(tagMapper::get)
			.collect(Collectors.toList());
	}

	private void associate(Cocktail cocktail, List<Tag> tags) {
		for (Tag tag : tags) {
			CocktailTag.associate(cocktail, tag);
		}
	}

	@Transactional(readOnly = true)
	public CocktailResponse findCocktailOfToday() {
		DailyDate dailyDate = DailyDate.of(new Date());
		CocktailSearcher cocktailSearcher = cocktailFindStrategyFactory.createCocktailSearcher(
			NumberOfDaily.generateBy(dailyDate));

		List<Cocktail> cocktails = cocktailRepository.findAll();

		Cocktail cocktailOfToday = cocktailSearcher.findIn(cocktails);
		return CocktailResponse.of(cocktailOfToday);
	}

	@Transactional(readOnly = true)
	public List<CocktailResponse> findByNameContaining(String name) {
		List<Cocktail> cocktailsContainingName = cocktailRepository.findByNameContaining(name);
		return CocktailResponse.listOf(cocktailsContainingName);
	}

	@Cacheable(value = "temp", key = "#id")
	public Temp findTemp(Long id) throws InterruptedException {
		System.err.println("Find ID : " + id);
		Thread.sleep(3 * 1000);

		return new Temp(id);
	}

	@CachePut(value = "temp", key = "#id")
	public Temp updateTemp(Long id, Long tempNum) {
		return new Temp(tempNum);
	}
}
