
const ja = new Map([
    ['create-button', 'チャットチャンネル作成'],
    ['input-label-channelName', 'チャンネル名'],
    ['subtitle', '完全匿名・プライベートなWeb Chat'],
    ['description', 'ユーザーアカウント不要。個人情報収集なし。メッセージ履歴なし。ブラウザ上で暗号化。'],
    ['feature-item-1', 'ユーザーアカウントを作らずに、任意の相手とチャットができます。'],
    ['feature-item-2', 'チャットチャンネル（チャットルーム）を作成する際、チャット中、一切個人情報を収集しません。'],
    ['feature-item-3', 'メッセージはサーバー上に保管されません。メッセージは全てブラウザ上で表示されるのみです。'],
    ['feature-item-4', 'メッセージの暗号化は、ブラウザ上で行われます。（秘密鍵を削除すれば、運営側でも復号はできません!）'],
    ['feature-item-5', 'メッセージは約20秒ごとに自動的にブラウザ上からも削除されます。さらに、10メッセージのみ表示され、古いメッセージから自動的に削除されます。'],
    ['feature-item-6', 'サーバー上のチャットチャンネルに関する情報は暗号化、もしくはハッシュ化されて保存されています（タイムスタンプとテーブルの主キーのみ暗号化されていません。ただ、タイムスタンプと主キーは、仕組み上システムが使うだけのもので、チャットの内容や相手の特定には無関係です。）'],
    ['feature-item-7', 'チャットを終了すれば、完全にチャットメッセージ・チャットチャンネルを復元できません。'],
    ['feature-item-8', 'オープンソースで、コードも公開しています。「本当に完全にプライペートなのか」、「仕組みが正しいか」、「クリティカルなバグはないのか」など確認できます!'],
    ['feature-item-9', '問題があればTwitter、GitHubで報告していただければ修正します!'],
    ['terms-of-service-title', '利用規約'],
    // The section below is generated by a script not included in this project yet. Please do not edit the code below.
    ['chapter-0', '本利用規約（以下「本規約」と言います。）には、本サービスの提供条件及び運営者とユーザーの皆様との間の権利義務関係が定められています。本サービスの利用に際しては、本規約の全文をお読みいただいた上で、本規約に同意いただく必要があります。'],
    ['chapter-h-1', '第1条（適用）'],
    ['tos-1-1', '1. 本規約は、本サービスの提供条件及び本サービスの利用に関する運営者とユーザーとの間の権利義務関係を定めることを目的とし、ユーザーと運営者との間の本サービスの利用に関わる一切の関係に適用されます。'],
    ['tos-1-2', '2. 運営者が運営ウェブサイト上で掲載する本サービス利用に関するルール（https://suzumechunchun.com）は、本規約の一部を構成するものとします。'],
    ['tos-1-3', '3. 本規約の内容と、前項のルールその他の本規約外における本サービスの説明等とが異なる場合は、本規約の規定が優先して適用されるものとします。'],
    ['chapter-h-2', '第２条（定義）'],
    ['tos-2-1', '本規約において使用する以下の用語は、各々以下に定める意味を有するものとします。'],
    ['tos-2-2', '(1) 「サービス利用契約」とは、本規約及び運営者とユーザーの間で締結する、本サービスの利用契約と意味します。'],
    ['tos-2-3', '(2) 「知的財産権」とは、著作権、特許権、実用新案権、意匠権、商標権その他の知的財産権（それらの権利を取得し、またはそれらの権利につき登録等を出願する権利を含みます。）を意味します。'],
    ['tos-2-4', '(3) 「送信メッセージ」とは、ユーザーが本サービスを利用して送信するコンテンツ（文章、画像、動画その他のデータを含みますがこれらに限りません。）を意味します。'],
    ['tos-2-5', '(4) 「運営者」とは、yrichikaを意味します。'],
    ['tos-2-6', '(5) 「運営ウェブサイト」とは、そのドメインが「suzumechunchun.com」である、運営者が運営するウェブサイト（理由の如何を問わず、運営者のウェブサイトのドメインまたは内容が変更された場合は、運営者変更後のウェブサイトを含みます。）を意味します。'],
    ['tos-2-7', '(6) 「ユーザー」とは、第３条（利用）に基づいて本サービスの利用者としての個人または法人を意味します。'],
    ['tos-2-8', '(7) 「本サービス」とは、運営者が提供するSuzumeChunChunという名称のサービス（理由の如何を問わずサービスの名称または内容が変更された場合は、当該変更後のサービスを含みます。）を意味します。'],
    ['chapter-h-3', '第3条(利用)'],
    ['tos-3-1', '1. 本サービスの利用を希望する者(以下「利用希望者」といいます。)は、本規約を遵守することに同意し、かっ運営者の定める一定の情報(以下「登録事項」といいます。)を運営者の定める方法で運営者に提供することにより、運営者に対し、本サービスの利用の登録を申請することができます。'],
    ['tos-3-2', '2. 運営者は、運営者の基準に従って、第1項に基づいて利用申請を行った利用希望者(以下「利用申請者」といいます。)の登録の可否を判断し、運営者が登録を認める場合にはその旨を利用申請者に通知します。利用申請者のユ一ザーとしての登録は、 運営者が本項の通知を行ったことをもって完了したものとします。'],
    ['tos-3-3', '3. 前項に定める利用の完了時に、サービス利用契約がユーザーと運営者の間に成立し、ユーザーは本サービスを本規約に従い利用することができるようになります。'],
    ['tos-3-4', '4. 運営者は、利用申請者が、以下の各号のいずれかの事由に該当する場合は、利用を拒否することがあり、またその理由について一切開示義務を負いません。'],
    ['tos-3-5', '(1) 未成年者、 成年被後見人、 被保佐人または被補助人のいずれかであり、 法定代理人、 後見人、 保佐人または補助人の同意等を得ていなかった場合'],
    ['tos-3-6', '(2) 反社会的勢力等 (暴力団、 暴力団員、 右翼団体、 反社会的勢力、 その他これに準ずる者を意味します。 以下同じ。) である、または資金提供その他を通じて反社会的勢力等の維持、運営もしくは経営に協力もしくは関与する等反社会的勢力等との何らかの交流もしくは関与を行っていると運営者が判断した場合'],
    ['tos-3-7', '(3) 利用希望者が過去運営者との契約に違反した者またはその関係者であると運営者が判断した場合'],
    ['tos-3-8', '(4) 第9条に定める措置を受けたことがある場合'],
    ['tos-3-9', '(5) その他、運営者が利用を適当でないと判断した場合'],
    ['chapter-h-4', '第4条(パスワード及びユーザー名の管理)'],
    ['tos-4-1', '1. ユーザーは、自己の責任において、本サービスに関する「合言葉（パスフレーズ）」及びユーザー名を適切に管理及び保管するものとし、これを第三者に利用させ、または貸与、譲渡、名義変更、売買等をしてはならないものとします。'],
    ['tos-4-2', '2. 合言葉（パスフレーズ）またはユーザー名の管理不十分、使用上の過誤、第三者の使用等によって生じた損害に関する責任はユーザーが負うものとし、運営者は一切の責任を負いません。'],
    ['chapter-h-5', '第5条(禁止事項)'],
    ['tos-5-1', 'ユーザーは、 本サービスの利用にあたり、以下の各号のいずれかに該当する行為または該当すると運営者が判断する行為をしてはなりません。'],
    ['tos-5-2', '(1) 法令に違反する行為または犯罪行為に関連する行為'],
    ['tos-5-3', '(2) 運営者、本サービスの他の利用者またはその他の第三者に対する詐欺または脅迫行為'],
    ['tos-5-4', '(3) 公序良俗に反する行為'],
    ['tos-5-5', '(4) 運営者、本サービスの他の利用者またはその他の第三者の知的財産権、肖像権、プライバシーの権利、名誉、その他の権利または利益を侵害する行為'],
    ['tos-5-6', '(5) 本サービスを通じ、以下に該当し、または該当すると運営者が判断する情報を運営者または本サービスの他の利用者に送信すること'],
    ['tos-5-7', '* 過度に暴力的または残虐な表現を含む情報'],
    ['tos-5-8', '* コンピュ一ター・ウイルスその他の有害なコンピューター・プログラムを含む情報'],
    ['tos-5-9', '* 運営者、本サービスの他の利用者またはその他の第三者の名誉または信用を毀損する表現を含む情報'],
    ['tos-5-10', '* 過度にわいせつな表現を含む情報'],
    ['tos-5-11', '* 差別を助長する表現を含む情報'],
    ['tos-5-12', '* 自殺、自傷行為を助長する表現を含む情報'],
    ['tos-5-13', '* 薬物の不適切な利用を助長する表現を含む情報'],
    ['tos-5-14', '* 反社会的な表現を含む情報'],
    ['tos-5-15', '* チェーンメール等の第三者への情報の拡散を求める情報'],
    ['tos-5-16', '* 他人に不快感を与える表現を含む情報'],
    ['tos-5-17', '* 面識のない異性との出会いを目的とした情報'],
    ['tos-5-18', '(6) 本サービスのネットワークまたはシステム等に過度な負荷をかける行為'],
    ['tos-5-19', '(7) 本サービスの運営を妨害するおそれのある行為'],
    ['tos-5-20', '(8) 運営者のネットワークまたはシステム等に不正にアクセスし、または不正なアクセスを試みる行為'],
    ['tos-5-21', '(9) 第三者に成りすます行為'],
    ['tos-5-22', '(10) 本サービスの他の利用者の合言葉、ユーザー名、チャンネル名を使い、なりすます行為'],
    ['tos-5-23', '(11) 運営者が事前に許諾しない本サービス上での宣伝、広告、勧誘、または営業行為'],
    ['tos-5-24', '(12) 本サービスの他の利用者の情報の収集'],
    ['tos-5-25', '(13) 運営者、本サービスの他の利用者またはその他の第三者に不利益、損害、不快感を与える行為'],
    ['tos-5-26', '(14) 運営ウェブサイト上で掲載する本サービス利用に関するルールに抵触する行為'],
    ['tos-5-27', '(15) 反社会的勢力等への利益供与'],
    ['tos-5-28', '(16) 面識のない異性との出会いを目的とした行為'],
    ['tos-5-29', '(17) 前各号の行為を直接または間接に惹起し、または容易にする行為'],
    ['tos-5-30', '(18) その他、運営者が不適切と判断する行為'],
    ['chapter-h-6', '第6条(本サービスの停止等)'],
    ['tos-6-1', '1. 運営者は、 以下のいずれかに該当する場合には、ユーザーに事前に通知することなく、本サービスの全部または一部の提供を停止または中断することができるものとします。'],
    ['tos-6-2', '(1) 本サービスに係るコンピューター・システムの点検または保守作業を緊急に行う場合'],
    ['tos-6-3', '(2) コンピューター、通信回線等が事故により停止した場合'],
    ['tos-6-4', '(3) 地震、落雷、火災、風水害 停電、天災地変などの不可抗力により本サービスの運営ができなくなった場合'],
    ['tos-6-5', '(4) その他、運営者が停止または中断を必要と判断した場合'],
    ['tos-6-6', '2. 運営者は、本条に基づき運営者が行った措置に基づきユーザーに生じた損害について一切の責任を負いません。'],
    ['chapter-h-7', '第7条(権利帰属)'],
    ['tos-7-1', '1. 運営ウェブサイト及び本サービスに関する知的財産権は全て運営者、または運営者にライセンスを許諾している者に帰属しており、本規約に基づく本サービスの利用許諾は、運営ウェブサイトまたは本サービスに関する運営者または運営者にライセンスを許諾している者の知的財産権の使用許諾を意味するものではありません。'],
    ['tos-7-2', '2.ユーザーは、送信メッセージについて、自らが送信することについての適法な権利を有していること、及びメッセージが第三者の権利を侵害していないことについて、運営者に対し表明し、保証するものとします。'],
    ['tos-7-3', '3. ユーザーは、運営者及び運営者から権利を承継しまたは許諾された者に対して著作者人格権を行使しないことに同意するものとします。'],
    ['chapter-h-8', '第8条(利用抹消等)'],
    ['tos-8-1', '1. 運営者は、ユーザーが、以下の各号のいずれかの事由に該当する場合は、事前に通知または催告することなく、当該ユーザーについて本サービスの利用を停止することができます。'],
    ['tos-8-2', '(1) 本規約のいずれかの条項に違反した場合'],
    ['tos-8-3', '(2) 運営者からの問いあわせその他の回答を求める連絡に対して2日間以上応答がない場合'],
    ['tos-8-4', '(3) 第3条 第4項各号に該当する場合'],
    ['tos-8-5', '(4) その他、運営者が本サービスの利用、ユーザーとしての利用、またはサービス利用契約の継続を適当でないと判断した場合'],
    ['tos-8-6', '2. 運営者は、本条に基づき運営者が行った行為によりユーザーに生じた損害について一切の責任を負いません。'],
    ['chapter-h-9', '第9条(本サービスの内容の変更、終了)'],
    ['tos-9-1', '1. 運営者は、運営者の都合により、本サービスの内容を変更し、または提供を終了することができます。運営者が本サービスの提供を終了する場合、運営者はユーザーに事前に通知するものとします。'],
    ['tos-9-2', '2. 運営者は、本条に基づき運営者が行った措置に基づきユーザーに生じた損害について一切の責任を負いません。'],
    ['chapter-h-10', '第10条 (保証の否認及び免責)'],
    ['tos-10-1', '1. 運営者は、本サービスがユーザーの特定の目的に適合すること、期待する機能・商品的価値・正確性・有用性を有すること、ユーザーによる本サービスの利用がユーザーに適用のある法令または業界団体の内部規則等に適合すること、及び不具合が生じないことについて、何ら保証するものではありません。'],
    ['tos-10-2', '2. 運営者は、運営者による本サービスの提供の中断、停止、終了、利用不能または変更、ユーザーが本サービスに送信したメッセージまたは情報の削除または消失、本サービスの利用による登録データの消失または機器の故障もしくは損傷、その他本サービスに関してユーザーが被った損害(以下「ユーザー損害」といいます。)につき、賠償する責任を一切負わないものとします。'],
    ['tos-10-3', '3. 何らかの理由により運営者が責任を負う場合であっても、運営者は、ユーザー損害につき、過去1ヶ月間にユーザーが運営者に支払った対価の金額を超えて賠償する責任を負わないものとし、また、付随的損害、間接損害、特別損害、将来の損害及び逸失利益にかかる損害については、賠償する責任を負わないものとします。'],
    ['tos-10-4', '4. 本サービスまたは運営ウェブサイトに関連してユーザーと他のユーザーまたは第三者との間において生じた取引、連絡、紛争等については、運営者は一切責任を負いません。'],
    ['chapter-h-11', '第11条(秘密保持)'],
    ['tos-11-1', 'ユーザーは、本サービスに関連して運営者がユーザーに対して秘密に取り扱うことを求めて開示した非公知の情報について、運営者の事前の書面による承諾がある場合を除き、秘密に取り扱うものとします。'],
    ['chapter-h-12', '第12条(利用者情報の取扱い)'],
    ['tos-12-1', '1. 運営者によるユーザーの利用者情報の取扱いについては、別途運営者プライバシーポリシーの定めによるものとし、ユーザーはこのプライバシーポリシーに従って運営者がユーザーの利用者情報を取扱うことについて同意するものとします。'],
    ['tos-12-2', '2.運営者は、ユーザーが運営者に提供した情報、データ等を、個人を特定できない形での統計的な情報として、運営者の裁量で、利用することができるものとし、ユーザーはこれに異議を唱えないものとします。'],
    ['chapter-h-13', '第13条(体規約等の変更)'],
    ['tos-13-1', '運営者は、本規約を変更できるものとします。運営者は、本規約を変更した場合には、ユーザーに当該変更内容を通知するものとし、当該変更内容の通知後、ユーザーが本サービスを利用した場合には、ユーザーは、 本規約の変更に同意したものとみなします。'],
    ['chapter-h-14', '第14条(連絡/通知)'],
    ['tos-14-1', '本サービスに関する問い合わせその他ユーザーから運営者に対する連絡または通知、及び本規約の変更に関する通知その他運営者からユーザーに対する連絡または通知は、運営者の定める方法で行うものとします。'],
    ['chapter-h-15', '第15条 (サービス利用契約上の地位の譲渡等)'],
    ['tos-15-1', '1. ユーザーは、運営者の書面による事前の承諾なく、サービス利用契約上の地位または本規約に基づく権利もしくは義務につき、第三者に対し、譲渡、移転、担保設定、その他の処分をすることはできません。'],
    ['tos-15-2', '2. 運営者は本サービスにかかる事業を他人格に譲渡した場合には、当該事業譲渡に伴いサービス利用契約上の地位、本規約に基づく権利及び義務を当該事業譲渡の譲受人に譲渡することができるものとし、ユーザーは、かかる譲渡につき本項において予め同意したものとします。なお、本項に定める事業譲渡には、通常の事業譲渡のみならず、会社分割その他事業が移転するあらゆる場合を含むものとします。'],
    ['chapter-h-16', '第16条(分離可能性)'],
    ['tos-16-1', '本規約のいずれかの条項またはその一部が、消費者契約法その他の法令等により無効または執行不能と判断された場合であっても、本規約の残りの規定及び一部が無効または執行不能と判断された規定の残りの部分は、継続して完全に効力を有するものとします。'],
    ['chapter-h-17', '第17条(準拠法及び管轄裁判所)'],
    ['tos-17-1', '1. 本規約及びサービス利用契約の準拠法は日本法とします。なお、本サービスにおいて物品の売買が発生する場合であっても、国際物品売買契約に関する国際連合条約の適用を排除することに合意します。'],
    ['tos-17-2', '2. 本規約またはサービス利用契約に起因し、または関連する一切の紛争については、名古屋地方裁判所を第一審の専属的合意管轄裁判所とします。'],
    ['tos-created-at', '2021年6月24日制定'],

]);