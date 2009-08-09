package com.atteo.langleo;

import com.atteo.langleo.models.Question;

public interface LearningAlgorithm {
	public void start();
	public void stop();
	public void answer(Question question, int answerQuality);
	public String isQuestionWaiting();
	public Question getQuestion();
	public int questionsAnswered();
	public int allQuestions();
	public void decreaseMax();
}
