package com.game.service;

import com.game.controller.PlayerOrder;
import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.exception.PlayerNoFoundException;
import com.game.repository.PlayerRepository;

import static com.game.repository.PlayerSpecification.*;

import com.game.validation.PlayerValidation;
import com.game.validation.PlayerValidationImpl;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service

public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository repository;


    @Autowired
    public PlayerServiceImpl(PlayerRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Player> getAllPlayers(String name, String title, Race race, Profession profession,
                                      Long after, Long before, Boolean banned, Integer minExperience,
                                      Integer maxExperience, Integer minLevel, Integer maxLevel,
                                      PlayerOrder order, Integer pageNumber, Integer pageSize) {


        Sort sort = Sort.by(order.getFieldName());
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, sort);

        return repository.findAll(hasName(name)
                        .and(hasTitle(title))
                        .and(hasRace(race))
                        .and(hasProfession(profession))
                        .and(hasBirthday(after, before))
                        .and(hasBanned(banned))
                        .and(hasExperience(minExperience, maxExperience))
                        .and(hasLevel(minLevel, maxLevel)), pageRequest)
                .stream().collect(Collectors.toList());
    }

    @Override
    public Player getPlayer(long id) {
        existId(id);
        return repository.findById(id).get();
    }

    @Override
    public Player updatePlayer(long id, Player player) {

        existId(id);

        Player playerUpdateInBase = repository.findById(id).get();

        PlayerValidation playValidUp = new PlayerValidationImpl(player);

        if (playValidUp.isNameNull())
            player.setName(playerUpdateInBase.getName());
        else
            playValidUp.isNameValid();

        if (playValidUp.isTitleNull())
            player.setTitle(playerUpdateInBase.getTitle());
        else
            playValidUp.isTitleValid();


        if (playValidUp.isRaceNull())
            player.setRace(playerUpdateInBase.getRace());

        if (playValidUp.isProfessionNull())
            player.setProfession(playerUpdateInBase.getProfession());

        if (playValidUp.isBirthdayNull())
            player.setBirthday(playerUpdateInBase.getBirthday());
        else
            playValidUp.isBirthdayValid();

        if (playValidUp.isBannedNull())
            player.setBanned(playerUpdateInBase.isBanned());

        if (playValidUp.isExperienceNull()) {
            player.setExperience(playerUpdateInBase.getExperience());
            player.setLevel(playerUpdateInBase.getLevel());
            player.setUntilNextLevel(playerUpdateInBase.getUntilNextLevel());
        } else {
            playValidUp.isExperienceValid();
            player.setLevel(calculateLevel(player.getExperience()));
            player.setUntilNextLevel(calculateNextLevel(player.getExperience(), player.getLevel()));
        }

        player.setId(playerUpdateInBase.getId());

        return repository.save(player);
    }

    @Override
    public Player createPlayer(Player player) {

        PlayerValidation playerValidationUpdateParam = new PlayerValidationImpl(player);
        playerValidationUpdateParam.allParamNotNull();
        playerValidationUpdateParam.allParamValid();

        player.setLevel(calculateLevel(player.getExperience()));
        player.setUntilNextLevel(calculateNextLevel(player.getExperience(), player.getLevel()));

        return repository.save(player);
    }


    public void deletePlayer(long id) {
        existId(id);
        repository.deleteById(id);
    }

    private void existId(long id) {
        if (!repository.existsById(id))
            throw new PlayerNoFoundException("Не найден игрок по id " + id);
    }

    private int calculateLevel(int exp) {
        return ((int) Math.sqrt(2500 + 200 * exp) - 50) / 100;
    }

    private int calculateNextLevel(int exp, int level) {
        return 50 * (level + 1) * (level + 2) - exp;
    }

}