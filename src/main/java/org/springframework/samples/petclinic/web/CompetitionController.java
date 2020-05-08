
package org.springframework.samples.petclinic.web;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.CredentialException;
import javax.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.samples.petclinic.model.Calendary;
import org.springframework.samples.petclinic.model.Competition;
import org.springframework.samples.petclinic.model.FootballClub;
import org.springframework.samples.petclinic.model.Jornada;
import org.springframework.samples.petclinic.model.Match;
import org.springframework.samples.petclinic.model.Enum.CompetitionType;
import org.springframework.samples.petclinic.model.Enum.MatchStatus;
import org.springframework.samples.petclinic.service.CompetitionService;
import org.springframework.samples.petclinic.service.FootballClubService;
import org.springframework.samples.petclinic.service.MatchService;
import org.springframework.samples.petclinic.service.RefereeService;
import org.springframework.samples.petclinic.service.exceptions.DuplicatedNameException;
import org.springframework.samples.petclinic.service.exceptions.NotEnoughMoneyException;
import org.springframework.samples.petclinic.service.exceptions.StatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CompetitionController {

	private static final String			VIEWS_COMPETITION_CREATE_OR_UPDATE_FORM	= "competitions/createOrUpdateCompetitionForm";

	@Autowired
	private final CompetitionService	competitionService;

	@Autowired
	private final FootballClubService	footballClubService;

	@Autowired
	private final RefereeService		refereeService;

	@Autowired
	private final MatchService			matchService;


	@Autowired
	public CompetitionController(final CompetitionService competitionService, final MatchService matchService, final FootballClubService footballClubService, final RefereeService refereeService) {
		this.competitionService = competitionService;
		this.footballClubService = footballClubService;
		this.refereeService = refereeService;
		this.matchService = matchService;
	}

	@InitBinder
	public void setAllowedFields(final WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	@ModelAttribute("types")
	public List<CompetitionType> populateTypes() {
		List<CompetitionType> types = new ArrayList<>();
		types.add(CompetitionType.LEAGUE);
		types.add(CompetitionType.PLAYOFFS);
		return types;
	}

	@ModelAttribute("status")
	public List<Boolean> populateStatus() {
		List<Boolean> status = new ArrayList<>();
		status.add(true);
		status.add(false);
		return status;
	}

	@GetMapping("/competitions/{competitionId}") //VISTA DETALLADA DE COMPETICIÓN
	public ModelAndView showCompetition(@PathVariable("competitionId") final int competitionId) {

		ModelAndView mav = new ModelAndView("competitions/competitionDetails");
		mav.addObject(this.competitionService.findCompetitionById(competitionId));
		mav.addObject("size", this.competitionService.findCompetitionById(competitionId).getClubs().size());

		return mav;
	}

	@GetMapping("/competitions/{competitionId}/calendary") //VISTA DETALLADA DE CALENDARIO
	public ModelAndView showCalendary(@PathVariable("competitionId") final int competitionId) {

		ModelAndView mav = new ModelAndView("competitions/calendaryDetails");
		mav.addObject(this.competitionService.findCalendaryByCompetitionId(competitionId));
		mav.addObject("jornadas", this.competitionService.findAllJornadasFromCompetitionId(competitionId));

		return mav;
	}

	@GetMapping("/competitions/{competitionId}/calendary/jornada/{jornadaId}/match/{matchId}") //VISTA DETALLADA DE PARTIDO
	public ModelAndView showMatch(@PathVariable("matchId") final int matchId) throws CredentialException {

		Match match = this.matchService.findMatchById(matchId);

		ModelAndView mav = new ModelAndView("competitions/matchDetails");

		mav.addObject(match);

		return mav;
	}

	@GetMapping("/competitions/{competitionId}/calendary/jornada/{jornadaId}") //VISTA DETALLADA DE JORNADA
	public ModelAndView showJornada(@PathVariable("jornadaId") final int jornadaId) {

		ModelAndView mav = new ModelAndView("competitions/jornadasDetails");
		mav.addObject(this.competitionService.findJornadaById(jornadaId));
		mav.addObject("partidos", this.competitionService.findAllMatchByJornadaId(jornadaId));
		return mav;
	}

	@GetMapping(value = "/competitions/list") //LISTA DE COMPETICIONES PUBLICADAS
	public String showCompetitionList(final Map<String, Object> model) {

		List<Competition> competitions = new ArrayList<>();
		competitions.addAll(this.competitionService.findAllPublishedCompetitions());

		model.put("competitions", competitions);

		return "competitions/competitionList";
	}

	@GetMapping(value = "/competition/mylist") //LISTA DE MIS COMPETICIONES
	public String showMyCompetitionList(final Map<String, Object> model) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String currentPrincipalName = authentication.getName();

		List<Competition> competitions = new ArrayList<>();
		competitions.addAll(this.competitionService.findMyCompetitions(currentPrincipalName));

		model.put("competitions", competitions);

		return "competitions/competitionList";
	}

	@GetMapping(value = "/competition/new") //CREAR COMPETICIÓN - GET
	public String initCreationForm(final Map<String, Object> model) {

		Competition competition = new Competition();
		model.put("competition", competition);
		model.put("isNew", true);
		return CompetitionController.VIEWS_COMPETITION_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping(value = "/competition/new") //CREAR COMPETICIÓN - POST
	public String processCreationForm(@Valid final Competition competition, final BindingResult result, final Model model) throws DataAccessException, StatusException {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String currentPrincipalName = authentication.getName();

		if (result.hasErrors()) {
			model.addAttribute("competition", competition);
			model.addAttribute("isNew", true);
			return CompetitionController.VIEWS_COMPETITION_CREATE_OR_UPDATE_FORM;
		} else {

			try {
				competition.setStatus(false);
				competition.setCreator(currentPrincipalName);
				this.competitionService.saveCompetition(competition);
			} catch (DuplicatedNameException e) {
				result.rejectValue("name", "duplicate", "already exists");
				return CompetitionController.VIEWS_COMPETITION_CREATE_OR_UPDATE_FORM;
			} catch (NotEnoughMoneyException e) {
				result.rejectValue("reward", "duplicate", "Cantidad de recompensa mínima: 5.000.000 €");
				return CompetitionController.VIEWS_COMPETITION_CREATE_OR_UPDATE_FORM;
			}
		}
		return "redirect:/competitions/" + competition.getId();
	}

	@GetMapping(value = "/competition/{competitionId}/edit") //EDITAR COMPETITION - GET
	public String initUpdateCompetitionForm(@PathVariable("competitionId") final int compId, final Map<String, Object> model) throws CredentialException {

		Competition comp = this.competitionService.findCompetitionById(compId);

		model.put("competition", comp);
		model.put("isEditing", true);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String currentPrincipalName = authentication.getName();

		if (!currentPrincipalName.equals(comp.getCreator()) || comp.getStatus() == true) { //SEGURIDAD
			throw new CredentialException("Forbidden Access");
		}

		return CompetitionController.VIEWS_COMPETITION_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping(value = "/competition/{competitionId}/edit") //EDITAR COMPETICION - POST
	public String processUpdateFootballClubForm(@Valid final Competition competition, final BindingResult result, @PathVariable("competitionId") final Integer compId, final Model model) throws DataAccessException, CredentialException, StatusException {

		model.addAttribute("isEditing", true);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String currentPrincipalName = authentication.getName();

		Competition compToUpdate = this.competitionService.findCompetitionById(compId);

		if (!currentPrincipalName.equals(compToUpdate.getCreator())) { //SEGURIDAD
			throw new CredentialException("Forbidden Access");
		}

		if (result.hasErrors()) {
			return CompetitionController.VIEWS_COMPETITION_CREATE_OR_UPDATE_FORM;
		} else {

			BeanUtils.copyProperties(competition, compToUpdate, "id", "creator", "clubs", "status");

			try {

				this.competitionService.saveCompetition(compToUpdate);
			} catch (NotEnoughMoneyException e) {
				result.rejectValue("reward", "duplicate", "Cantidad de recompensa mínima: 5.000.000 €");
				return CompetitionController.VIEWS_COMPETITION_CREATE_OR_UPDATE_FORM;
			} catch (DuplicatedNameException e) {
				result.rejectValue("name", "duplicate", "already exists");
				return CompetitionController.VIEWS_COMPETITION_CREATE_OR_UPDATE_FORM;
			}

			return "redirect:/competitions/" + compId;

		}

	}

	@GetMapping("/competition/{competitionId}/addClubs") //AÑADIR EQUIPOS A COMPETICIÓN
	public ModelAndView showClubs(@PathVariable("competitionId") final int competitionId) {

		Collection<String> allclubsName = this.competitionService.findClubsById(competitionId);

		Competition thisComp = this.competitionService.findCompetitionById(competitionId);

		ModelAndView mav = new ModelAndView("competitions/listClubs");
		mav.addObject("clubsName", allclubsName);
		mav.addObject("isAdd", true);
		mav.addObject("size", allclubsName.size());
		mav.addObject(thisComp);

		return mav;
	}
	@PostMapping("/competition/{competitionId}/addClubs") //AÑADIR EQUIPOS A COMPETICIÓN
	public ModelAndView addClub(@PathVariable("competitionId") final int competitionId, @ModelAttribute("clubs") final String club) throws DataAccessException, DuplicatedNameException {

		Competition thisComp = this.competitionService.findCompetitionById(competitionId);

		List<String> newClubs = thisComp.getClubs();
		newClubs.add(club);
		thisComp.setClubs(newClubs);

		try {
			this.competitionService.saveCompetition(thisComp);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ModelAndView mav = new ModelAndView("redirect:/competitions/" + competitionId);

		return mav;
	}

	@GetMapping("/competition/{competitionId}/clubs") //VER EQUIPOS DE LA COMPETICIÓN
	public ModelAndView showClubsMycomp(@PathVariable("competitionId") final int competitionId) {

		Competition thisComp = this.competitionService.findCompetitionById(competitionId);

		ModelAndView mav = new ModelAndView("competitions/listClubs");
		mav.addObject("clubsName", thisComp.getClubs());
		mav.addObject("size", thisComp.getClubs().size());
		mav.addObject("isAdd", false);
		mav.addObject(thisComp);

		return mav;
	}

	@PostMapping("/competition/{competitionId}/clubs") //BORRAR EQUIPOS A COMPETICIÓN
	public ModelAndView deleteClub(@PathVariable("competitionId") final int competitionId, @ModelAttribute("clubs") final String club) throws DataAccessException, DuplicatedNameException, NotEnoughMoneyException, StatusException {

		Competition thisComp = this.competitionService.findCompetitionById(competitionId);

		List<String> newClubs = thisComp.getClubs();
		newClubs.remove(club);
		thisComp.setClubs(newClubs);

		this.competitionService.saveCompetition(thisComp);

		ModelAndView mav = new ModelAndView("redirect:/competitions/" + competitionId);

		return mav;
	}

	@RequestMapping(value = "/competition/{competitionId}/publish") //PUBLICAR COMPETITION
	public String initPublishCompetitionForm(@PathVariable("competitionId") final int compId, final Model model) throws CredentialException, DataAccessException, DuplicatedNameException, NotEnoughMoneyException, StatusException {

		Competition comp = this.competitionService.findCompetitionById(compId);

		model.addAttribute("competition", comp);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String currentPrincipalName = authentication.getName();

		if (!currentPrincipalName.equals(comp.getCreator()) || comp.getStatus() == true) { //SEGURIDAD
			throw new CredentialException("Forbidden Access");
		}

		if (comp.getType().equals(CompetitionType.LEAGUE)) {

			try {
				comp.setStatus(true);
				this.competitionService.saveCompetition(comp);
			} catch (StatusException e) {
				model.addAttribute("statusError", true);
				model.addAttribute("competition", comp);
				model.addAttribute("size", comp.getClubs().size());
				return "/competitions/competitionDetails";
			}

			List<String> equipos = comp.getClubs();
			List<String> equipos2 = comp.getClubs();
			Collections.reverse(equipos2);

			Date fechaPartido = new Date(System.currentTimeMillis() - 1);

			List<Match> competitionMatches = new ArrayList<Match>();

			//Creamos el calendario

			Calendary calendary = new Calendary();
			calendary.setCompetition(comp);
			this.competitionService.saveCalendary(calendary);

			//Creamos las jornadas

			Integer N = equipos.size();

			for (int i = 0; i < N * 2 - 2; i++) {

				int numero = i + 1;
				Jornada j = new Jornada();
				j.setCalendary(calendary);
				j.setName("Jornada " + numero);
				this.competitionService.saveJornada(j);
			}

			Collection<Jornada> jornadas = this.competitionService.findAllJornadasFromCompetitionId(compId);

			for (Jornada a : jornadas) {

				int contador = 1;
				int i = 0;
				int j = equipos.size() - 1;

				FootballClub club = new FootballClub();
				FootballClub club2 = new FootballClub();

				while (contador <= equipos.size() / 2) {

					club = this.footballClubService.findFootballClubByName(equipos.get(i));
					club2 = this.footballClubService.findFootballClubByName(equipos.get(j));

					Match newMatch = new Match();

					Calendar cal = Calendar.getInstance();
					cal.setTime(fechaPartido);
					cal.add(Calendar.HOUR, 2);
					fechaPartido = cal.getTime();

					newMatch.setCreator(currentPrincipalName);
					newMatch.setFootballClub1(club);
					newMatch.setFootballClub2(club2);
					newMatch.setMatchDate(fechaPartido);
					newMatch.setMatchStatus(MatchStatus.TO_BE_PLAYED);
					newMatch.setStadium(club.getStadium());
					newMatch.setTitle("Partido de Liga");
					newMatch.setReferee(this.refereeService.findRefereeById(1));
					newMatch.setJornada(a);

					this.competitionService.saveMatch(newMatch);

					contador++;
					i++;
					j--;

				}

				Collections.rotate(equipos, 1);

				Calendar cal = Calendar.getInstance();
				cal.setTime(fechaPartido);

				cal.add(Calendar.HOUR, 44);
				fechaPartido = cal.getTime();

			}
		}

		this.competitionService.saveCompetition(comp);

		return "redirect:/competitions/" + compId;
	}

}
