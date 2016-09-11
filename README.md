# twitter_notifier
指定のキーワードが見つかったらDMで通知する情報収集用アプリ

使い方
------

1. https://dev.twitter.com/ でManage Your Appsから適当なプロジェクトを作成  
  Keys and AccessTokenからAccess levelをDirectMessageを許可するように修正し、アクセストークンを作成しておく
2. jarを作成  
  cloneした後、EclipseでインポートしてRunnable JAR fileでjarを生成
3. 作成したjarを適当な箇所に配置  
  /home/hogehoge/twitter_notifier.jarに置いたとする
4. twitter4j.properties, search.propertiesをjarと同じ場所に置き、項目を埋める  
  ※accessToken、accessTokenSecretは冒頭にタブが入るのが正しいので注意
5. cronで定期的に呼ばれるようにする  
  10分ごとに実行する例  
    */10 * * * * java -jar /home/hogehoge/twitter_notifier.jar

これでsearch.propertiesに指定したアカウントに対し、検索結果がDMで飛んでくる。  
DMの内容は「Tweetのアドレス Tweet日時 Tweet内容」となっている
