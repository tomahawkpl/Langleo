package com.atteo.langleo;

import com.atteo.langleo.models.Question;

public interface LearningAlgorithm {
	public static int QUESTIONS_WAITING = 0;
	public static int NO_QUESTIONS = 1;
	public static int QUESTIONS_ANSWERED = 2;
	public static int QUESTIONS_ANSWERED_FORCEABLE = 3;
	 
	public void start();
	public void stop();
	
	public int isQuestionWaiting();
	
	public void answer(Question question, int answerQuality);
	public Question getQuestion();
	public int questionsAnswered();
	public int allQuestions();
	
	public void deletedQuestion(Question question);
}
