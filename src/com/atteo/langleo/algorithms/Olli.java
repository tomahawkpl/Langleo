package com.atteo.langleo.algorithms;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Random;

import android.content.SharedPreferences;

import com.atteo.langleo.Langleo;
import com.atteo.langleo.LearningAlgorithm;
import com.atteo.langleo.models.Collection;
import com.atteo.langleo.models.List;
import com.atteo.langleo.models.OlliAnswer;
import com.atteo.langleo.models.OlliFactor;
import com.atteo.langleo.models.Question;
import com.atteo.langleo.models.StudyDay;
import com.atteo.langleo.models.StudySession;
import com.atteo.langleo.models.Word;
import com.atteo.langleo.util.BetterAsyncTask;
import com.atteo.silo.StorableCollection;

public class Olli implements LearningAlgorithm {
	private int newWordsToday;
	private int newWordsInThisSession;
	private int maxNewWordsPerDay;
	private int maxNewWordsPerSession;
	private int allQuestions;
	private int questionsAnswered;

	private int prioritySum;
	private ArrayList<Integer> wordsCount;
	private ArrayList<Integer> priorities;
	private ArrayList<Boolean> started;

	private int currentCollection;

	private Random random;

	private ArrayList<Collection> collections;
	private HashMap<Integer, ArrayList<Question>> questions;
	private HashMap<Integer, ArrayList<Question>> laterQuestions;
	private ArrayList<Integer> newQuestionsPerCollection;

	private static final long FIRST_INTERVAL = 1000 * 60 * 60;
	private static final float FIRST_FACTOR = (float) 4;
	private static final float MAX_FACTOR_DIFFERENCE = (float) 0.2;

	private static final float BASE_FACTOR_DIFFERENCE = (float) 0.05;
	private static final float FACTOR_CHANGE_SPEED_DIFFERENCE = (float) 10;
	
	private long lastCheck;

	private StudyDay studyDay;
	private StudySession studySession;

	@Override
	public void start() {
		SharedPreferences prefs = Langleo.getPreferences();
		maxNewWordsPerDay = Integer.valueOf(prefs.getString(
				"new_words_per_day", Langleo.DEFAULT_NEW_WORDS_PER_DAY));
		maxNewWordsPerSession = Integer
				.valueOf(prefs.getString("new_words_per_session",
						Langleo.DEFAULT_NEW_WORDS_PER_SESSION));
		studyDay = StudyDay.getToday();
		studyDay.load();
		newWordsToday = studyDay.getNewWords();

		studySession = StudySession.getThisSession();
		studySession.load();
		newWordsInThisSession = studySession.getNewWords();

		int maxNewWords = maxNewWordsPerDay - newWordsToday;
		if (maxNewWords > maxNewWordsPerSession - newWordsInThisSession)
			maxNewWords = maxNewWordsPerSession - newWordsInThisSession;

		random = new Random();
		random.setSeed(new GregorianCalendar().getTimeInMillis());

		prioritySum = 0;
		currentCollection = 0;
		priorities = new ArrayList<Integer>();
		wordsCount = new ArrayList<Integer>();
		started = new ArrayList<Boolean>();

		StorableCollection storableCollection = new StorableCollection(
				Collection.class);
		storableCollection.whereInPlace("disabled = 0");
		storableCollection.orderByInPlace("name");
		collections = storableCollection.toArrayList();

		Collection c;
		int len = collections.size();
		int notLearned;
		for (int i = 0; i < len; i++) {
			c = collections.get(i);
			started.add(c.getStarted() == null);
			notLearned = c.getNotLearnedWordsCount();
			wordsCount.add(notLearned);
			if (notLearned == 0) {
				priorities.add(0);
			} else {
				priorities.add(c.getPriority());
				prioritySum += c.getPriority();

			}
		}

		newQuestionsPerCollection = new ArrayList<Integer>();

		int p = prioritySum;
		int newQuestions;
		int m = maxNewWords;
		for (int i = 0; i < len; i++) {
			if (p > 0) {
				newQuestions = m * priorities.get(i) / p;
				if (newQuestions > wordsCount.get(i))
					newQuestions = wordsCount.get(i);
				m -= newQuestions;
				p -= priorities.get(i);
			} else
				newQuestions = 0;
			newQuestionsPerCollection.add(newQuestions);

		}

		questions = new HashMap<Integer, ArrayList<Question>>();
		laterQuestions = new HashMap<Integer, ArrayList<Question>>();

		questionsAnswered = 0;
		allQuestions = 0;

		lastCheck = -1;

		findNewQuestions();
		allQuestions += (maxNewWords - m) * 2;

	}

	private void findNewQuestions() {
		StorableCollection nextQuestionCollection = new StorableCollection(
				Question.class);
		Date d = new Date();
		nextQuestionCollection.whereInPlace("date > " + lastCheck
				+ " and date <= " + d.getTime());
		lastCheck = d.getTime();
		nextQuestionCollection.orderByInPlace("collection_id");
		ArrayList<Question> loadedQuestions = nextQuestionCollection
				.toArrayList();
		int len = loadedQuestions.size();
		int currentCollection = -1;
		ArrayList<Question> currentQuestionList = null;
		Question q;

		int extra = 0;
		int reserved = 0;
		for (int i = 0; i < len; i++) {
			q = loadedQuestions.get(i);
			if (q.getCollection().getId() != currentCollection) {
				reserved = 0;
				currentCollection = q.getCollection().getId();
				if (questions.get(currentCollection) == null)
					questions.put(currentCollection,
							currentQuestionList = new ArrayList<Question>());
				else
					currentQuestionList = questions.get(currentCollection);
			}
			if (q.getRepetitions() == -1) {
				extra++;
				reserved++;
				currentQuestionList.add(currentQuestionList.size(), q);
			} else
				currentQuestionList.add(random.nextInt(currentQuestionList
						.size()
						+ 1 - reserved), q);

		}

		allQuestions += len + extra;

	}

	@Override
	public void stop() {
		studyDay.setNewWords(newWordsToday);
		studyDay.save();
		studySession.setDate(new Date());
		studySession.setNewWords(newWordsInThisSession);
		studySession.save();

	}

	@Override
	public void answer(Question question, int answer) {
		new AnswerTask(question, answer).execute((Void)null);
		
	}

	private int intDifficulty(float difficulty) {
		return (int) Math.round(difficulty - 0.5);
	}

	private OlliFactor getOlliFactor(int repetitions, int difficulty) {
		OlliFactor result;
		StorableCollection sc = new StorableCollection(OlliFactor.class);
		sc.whereInPlace("repetitions = ? and difficulty = ?", new String[] {
				String.valueOf(repetitions), String.valueOf(difficulty) });
		result = sc.getFirst();

		if (result != null)
			return result;

		result = new OlliFactor();
		result.setDifficulty(difficulty);
		result.setRepetitions(repetitions);
		result.setFactor(FIRST_FACTOR);
		return result;
	}

	private void addOlliAnswer(int repetitions, int diff, float usedFactor,
			int answer) {

		float f = (float) Math.rint(usedFactor * 10) / 10;

		StorableCollection collection = new StorableCollection(OlliAnswer.class);
		collection.whereInPlace("repetitions = " + repetitions
				+ " and difficulty = " + diff + " and factor=" + f);

		OlliAnswer oa = collection.getFirst();
		if (oa == null) {
			oa = new OlliAnswer();
			oa.setDifficulty(diff);
			oa.setFactor(usedFactor);
			oa.setRepetitions(repetitions);
		}

		if (answer == 1)
			oa.setCorrect(oa.getCorrect() + 1);
		else
			oa.setIncorrect(oa.getIncorrect() + 1);

		oa.save();
	}

	private void updateOlliFactor(int repetitions, float difficulty,
			float usedFactor, int answer) {
		int diff = intDifficulty(difficulty);
		OlliFactor of = getOlliFactor(repetitions, diff);

		addOlliAnswer(diff, repetitions, usedFactor, answer);

		float difference;

		float newFactor = of.getFactor();

		if (answer == 1) {
			difference = usedFactor / of.getFactor();
			if (difference > 1)
				newFactor *= (difference + 19) / 20;
		} else {
			difference = BASE_FACTOR_DIFFERENCE;
			if (usedFactor > of.getFactor()) {
				difference /= (float) Math.pow(FACTOR_CHANGE_SPEED_DIFFERENCE,
						(usedFactor / of.getFactor() - 1));
				newFactor *= 1 - difference;
			} else {
				difference = 1 - difference;
				difference /= 2 - usedFactor / of.getFactor();
				newFactor *= difference;
			}
		}

		if (newFactor < 1.3)
			newFactor = (float) 1.3;
		of.setFactor(newFactor);
		of.setHits(of.getHits() + 1);
		of.save();
		// if ((allQuestions-questionsAnswered) % 5 == 0)
	}

	private float getFactor(int repetitions, float difficulty) {
		int diff = intDifficulty(difficulty);
		OlliFactor of = getOlliFactor(repetitions, diff);
		float factor = of.getFactor();
		Random r = new Random();
		r.setSeed(new Date().getTime());
		float random;
		random = (float) r.nextGaussian();
		while (Math.abs(random) > 1) {
			random = (float) r.nextGaussian();
		}
		float possibleChange = (float) (1000 - of.getHits()) / 1000;
		if (possibleChange < 0.1)
			possibleChange = (float) 0.1;
		float difference = possibleChange * (random * MAX_FACTOR_DIFFERENCE);
		if (difference < 0)
			difference *= 2;

		return factor + difference;
	}

	private void updateDifficulty(Question question, int answer) {
		int mult = question.getQueries();
		if (mult == 0) {
			if (answer == 0)
				question.setDifficulty((float) 3.5);
			else
				question.setDifficulty((float) 1.5);
			return;
		}
		float step = (float) 5.0 / (mult + 1);
		float newDifficulty = question.getDifficulty();
		if (answer == 1) {
			step /= 3;
			newDifficulty -= step;
		} else
			newDifficulty += step;

		if (newDifficulty < 0)
			newDifficulty = 0;
		if (newDifficulty > 5)
			newDifficulty = 5;
		question.setDifficulty(newDifficulty);
	}

	private Question newQuestionFromCollection(int collectionPosition) {
		if (newWordsToday >= maxNewWordsPerDay
				|| newWordsInThisSession >= maxNewWordsPerSession
				|| prioritySum == 0
				|| newQuestionsPerCollection.get(collectionPosition) == 0)
			return null;
		Word w = new StorableCollection(Word.class)
				.whereInPlace("studied = 0")
				.whereInPlace(
						"list_id in (select id from list where collection_id = ?)",
						new String[] { String.valueOf(collections.get(
								collectionPosition).getId()) }).getFirst();
		if (w == null)
			return null;
		if (started.get(collectionPosition)) {
			started.set(collectionPosition, false);
			Collection collection = collections.get(collectionPosition);
			collection.setStarted(new Date());
			collection.save();
		}

		newWordsToday += 1;
		newWordsInThisSession += 1;
		newQuestionsPerCollection.set(collectionPosition,
				newQuestionsPerCollection.get(collectionPosition) - 1);
		Question newQuestion = new Question();
		newQuestion.setDate(new Date(0));
		newQuestion.setWord(w);
		w.load();
		List l = w.getList();
		l.load();
		newQuestion.setCollection(l.getCollection());
		newQuestion.save();
		w.setStudied(true);
		w.save();
		return newQuestion;
	}

	@Override
	public Question getQuestion() {
		while (currentCollection < collections.size()) {
			Question q;
			int c = collections.get(currentCollection).getId();
			if (questions.get(c) != null && !questions.get(c).isEmpty()) {
				q = questions.get(c).get(0);
				questions.get(c).remove(0);
				return q;
			}

			q = newQuestionFromCollection(currentCollection);
			if (q != null) {
				return q;
			}

			if (laterQuestions.get(c) != null
					&& !laterQuestions.get(c).isEmpty()) {
				q = laterQuestions.get(c).get(0);
				laterQuestions.get(c).remove(0);
				return q;
			}
			

			currentCollection++;
		}
		return null;
	}

	@Override
	public int isQuestionWaiting() {
		StorableCollection nextQuestionCollection = new StorableCollection(
				Question.class);
		nextQuestionCollection.whereInPlace("date <= " + new Date().getTime());
		Question q = nextQuestionCollection.getFirst();
		if (q != null)
			return LearningAlgorithm.QUESTIONS_WAITING;

		SharedPreferences prefs = Langleo.getPreferences();
		maxNewWordsPerDay = Integer.valueOf(prefs.getString(
				"new_words_per_day", Langleo.DEFAULT_NEW_WORDS_PER_DAY));
		maxNewWordsPerSession = Integer
				.valueOf(prefs.getString("new_words_per_session",
						Langleo.DEFAULT_NEW_WORDS_PER_SESSION));
		StudyDay studyDay = StudyDay.getToday();
		studyDay.load();
		newWordsToday = studyDay.getNewWords();

		StudySession studySession = StudySession.getThisSession();
		studySession.load();
		newWordsInThisSession = studySession.getNewWords();

		boolean newPossible = false, newInDb = false;
		
		if (newWordsToday < maxNewWordsPerDay
				&& newWordsInThisSession < maxNewWordsPerSession)
			newPossible = true; 

		nextQuestionCollection = new StorableCollection(Collection.class);
		nextQuestionCollection.whereInPlace("priority > 0");
		nextQuestionCollection
				.whereInPlace("exists(select * from word where list_id in (select id from list where collection_id = collection.id) and studied=0)");
		Collection c = nextQuestionCollection.getFirst();
		if (c != null)
			newInDb = true;
		
		if (!newPossible && !newInDb)
			return LearningAlgorithm.QUESTIONS_ANSWERED;
		if (!newPossible && newInDb)
			return LearningAlgorithm.QUESTIONS_ANSWERED_FORCEABLE;
		
		if (newPossible && newInDb)
			return LearningAlgorithm.QUESTIONS_WAITING;
		
		nextQuestionCollection = new StorableCollection(Word.class);
		Word w = nextQuestionCollection.getFirst();
		if (w == null)
			return LearningAlgorithm.NO_QUESTIONS;
		else
			return LearningAlgorithm.QUESTIONS_ANSWERED;
	}

	@Override
	public int questionsAnswered() {
		return questionsAnswered;
	}

	@Override
	public int allQuestions() {
		return allQuestions;
	}

	@Override
	public void deletedQuestion(Question question) {
		allQuestions--;
		if (question.getRepetitions() == -1) {
			allQuestions--;
			newWordsToday -= 1;
			newWordsInThisSession -= 1;
			int collectionPosition = -1;
			for (int i = 0; i < collections.size(); i++)
				if (collections.get(i).getId() == question.getCollection()
						.getId()) {
					collectionPosition = i;
					break;
				}
			newQuestionsPerCollection.set(collectionPosition,
					newQuestionsPerCollection.get(collectionPosition) + 1);
		}

	}

	private class AnswerTask extends BetterAsyncTask<Void, Void, Void> {
		private Question question;
		private int answer;
		private int current;

		public AnswerTask(Question question, int answer) {
			this.question = question;
			this.answer = answer;
			
		}

		@Override
		protected void onPreExecute() {
			if (answer == 1 || answer == -1)
				questionsAnswered++;
			current = currentCollection;
			
			if (answer == -1) {
				int c = collections.get(current).getId();
				if (laterQuestions.get(c) == null)
					laterQuestions.put(c, new ArrayList<Question>());
				laterQuestions.get(c).add(
						random.nextInt(laterQuestions.get(c).size() + 1),
						question);
				return;
			}
			
			if (answer != 1) {
				int c = collections.get(current).getId();
				if (laterQuestions.get(c) == null)
					laterQuestions.put(c, new ArrayList<Question>());
				laterQuestions.get(c).add(
						random.nextInt(laterQuestions.get(c).size() + 1),
						question);

			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (answer == -1) {
				question.addRepetition();
				question.save();
				return null;
			}
			Date d = new Date();

			if (question.getPreviousInterval() == null) // bug
				question.zeroRepetitions();
			
			if (question.getRepetitions() > 0) {
				BigDecimal bd = BigDecimal.valueOf(d.getTime()
						- question.getPreviousDate().getTime());
				float factor = bd.divide(
						BigDecimal.valueOf(question.getPreviousInterval()), 5,
						BigDecimal.ROUND_FLOOR).floatValue();

				updateOlliFactor(question.getRepetitions(), question
						.getDifficulty(), factor, answer);
			}

			question.addQuery();
			question.addRepetition();

			updateDifficulty(question, answer);

			if (answer == 1) {
				question.addCorrect();
				float factor = getFactor(question.getRepetitions(), question
						.getDifficulty());

				if (question.getRepetitions() > 1) {
					question.setPreviousInterval(d.getTime()
							- question.getPreviousDate().getTime());
					question.setDate(new Date((long) (d.getTime() + factor
							* question.getPreviousInterval())));

				} else {
					question.setPreviousInterval(FIRST_INTERVAL);
					question.setDate(new Date(new Date().getTime()
							+ (long) (factor * FIRST_INTERVAL)));
				}
				question.setPreviousDate(new Date());
				question.save();
			} else {
				question.zeroRepetitions();
				question.setPreviousDate(new Date(0));
				question.save();
			}
			findNewQuestions();
			return null;
		}

	}

}
