package com.example.quickworktime.domain.usecase

/**
 * UseCase基底クラス
 * すべてのUseCaseはこのクラスを継承する
 */
abstract class BaseUseCase<in P, out R> {

	/**
	 * UseCaseの実行メソッド
	 * @param parameters 実行パラメータ
	 * @return 実行結果
	 */
	abstract suspend fun execute(parameters: P): R
}

/**
 * パラメータが不要なUseCase用の基底クラス
 */
abstract class BaseUseCaseNoParam<out R> {

	/**
	 * UseCaseの実行メソッド（パラメータなし）
	 * @return 実行結果
	 */
	abstract suspend fun execute(): R
}