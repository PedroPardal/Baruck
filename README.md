# 🐴 Baruck — Gerenciamento de estufas e safras

App Android (Kotlin + Jetpack Compose) com visual no estilo Duolingo, feito para
gerenciar **lavouras, estufas, safras e colheitas**, com login pelo Google,
banco de dados **Firebase (Firestore)**, sistema de **amigos**, compartilhamento
da lavoura entre membros e **notificações personalizadas** (push).

O mascote é o cavalinho **Baruck**.

---

## O que o app faz

- **Lavouras**: a unidade principal. Você cria quantas quiser e convida amigos para gerenciar junto.
- **Estufas**: cada lavoura tem suas estufas (nome, área em m², observação).
- **Safras**: cadastro com uma **grande variedade de culturas** (mais de 50: folhosas, temperos, frutos, raízes, frutas…). Cada safra fica dentro de uma estufa, com variedade, data de plantio e previsão de colheita calculada pelo ciclo da cultura.
- **Colheitas**: lançadas por safra, **divididas em caixas grandes, médias e miúdas**, cada uma com **preço** e **data**. O app soma tudo e calcula o faturamento.
- **Amigos**: busca por e-mail, pedido/aceite de amizade, e depois você adiciona o amigo como **membro** de uma lavoura.
- **Notificações**: lembretes por lavoura (plantio, adubação, colheita) com data; todos os membros recebem o push. Também há push ao receber pedido de amizade e ao entrar numa lavoura.

---

## Como gerar o APK (sem instalar nada no PC)

O projeto já vem com um **GitHub Actions** que compila o APK na nuvem.

1. Crie um repositório no GitHub e suba esta pasta.
2. Faça o setup do Firebase (seção abaixo) e baixe o `google-services.json`.
3. No GitHub, vá em **Settings → Secrets and variables → Actions → New repository secret**:
   - Nome: `GOOGLE_SERVICES_JSON`
   - Valor: cole **todo o conteúdo** do seu `google-services.json`.
4. Vá na aba **Actions**, rode o workflow **Build Baruck APK** (ou só dê um push).
5. Quando terminar, baixe o APK em **Artifacts → baruck-debug-apk**.
6. Passe o APK para o celular e instale (ative "Fontes desconhecidas").

> Preferindo compilar no PC: instale o **Android Studio**, coloque o
> `google-services.json` em `app/`, e rode **Run** ou `./gradlew assembleDebug`.

---

## Setup do Firebase (uma vez)

1. Acesse <https://console.firebase.google.com> e crie um projeto (ex.: "Baruck").
2. **Adicione um app Android**:
   - Nome do pacote: `com.pedro.baruck`
   - Baixe o **`google-services.json`** e salve em `app/google-services.json`
     (e use o conteúdo dele no secret do GitHub, como acima).
3. **Authentication** → **Sign-in method** → ative **Google**.
4. **Firestore Database** → **Criar banco de dados** → modo de produção.
5. **Regras**: copie o conteúdo de `firestore.rules` deste projeto e publique
   (ou use a CLI, abaixo). As regras garantem que:
   - cada um só vê as lavouras das quais é membro;
   - só o dono apaga a lavoura;
   - amigo só vira membro por convite.
6. **Chave do login Google (Web client ID)**:
   - No `google-services.json`, procure o `client_type: 3` (é o *Web client*),
     copie o `client_id` (termina em `.apps.googleusercontent.com`).
   - Cole em `app/src/main/res/values/strings.xml` no lugar de
     `COLE_AQUI_O_WEB_CLIENT_ID` (string `web_client_id`).
7. **SHA-1** (necessário para o login Google funcionar):
   - A keystore de debug já vem no projeto (`keystore/baruck-debug.jks`).
   - O SHA-1 dela é:
     `05:BB:84:88:96:A1:6A:41:B1:A0:50:EC:B9:C4:4B:83:B7:14:17:3C`
   - No Firebase → Configurações do projeto → seu app Android → **Adicionar
     impressão digital** → cole esse SHA-1.
   - (Se gerar o APK pelo GitHub com a keystore deste projeto, é esse mesmo SHA-1.)

---

## Notificações push (Cloud Functions)

As notificações saem de uma Cloud Function que observa o banco. Para ativar:

```bash
npm install -g firebase-tools
firebase login
cd functions && npm install && cd ..
firebase deploy --only functions,firestore:rules,firestore:indexes
```

> As Functions exigem o plano **Blaze** (tem cota grátis generosa; para o seu
> uso o custo tende a ser zero). Sem fazer o deploy das Functions, o app
> funciona todo — só os **pushes** não são enviados (os lembretes continuam
> visíveis dentro do app).

O que cada função faz (`functions/index.js`):
- `onInbox`: manda push quando algo cai no inbox do usuário.
- `onFriendRequest`: avisa quando alguém te manda pedido de amizade.
- `dueReminders`: a cada 15 min, dispara os lembretes que venceram.

---

## Estrutura do projeto

```
app/src/main/
  java/com/pedro/baruck/
    data/           modelos, catálogo de culturas e repositório Firestore
    auth/           login Google (Credential Manager)
    notify/         serviço de push (FCM)
    ui/theme/       cores, fontes e tema estilo Duolingo
    ui/components/  botões 3D, cartões, campos
    ui/screens/     login, lavouras, estufas/safras/colheitas, amigos, lembretes
  res/              ícone (Baruck), mascote em várias poses, fontes
functions/          Cloud Functions de notificação
firestore.rules     regras de segurança
.github/workflows/  build automático do APK
```

---

## Personalização rápida

- **Cores**: `ui/theme/Theme.kt` (o verde principal é `#58CC02`).
- **Culturas**: adicione itens em `data/CropCatalog.kt`.
- **Tamanhos de caixa**: hoje são Grande/Média/Miúda (`data/Models.kt`, enum `BoxSize`).
