# Análise de Vegetação em Estradas

Sistema para medir a altura da vegetação às margens de estradas a partir de fotos capturadas por um veículo em movimento, com análise computacional da altura e uma IA preditiva para estimar a próxima data de corte.

## Visão geral

O projeto tem três frentes:

1. **Dashboard web** — mapa com os pontos onde as fotos foram tiradas, recebimento e análise das fotos (medição da altura da grama) e uma IA preditiva, conectada a um banco de dados e a um modelo treinado, para prever a próxima data de corte.
2. **App mobile** (este repositório) — captura as fotos durante o trajeto e registra a coordenada de cada uma.
3. **Hardware** — uma câmera acoplada ao topo do veículo, conectada por fio ao tablet ou celular que roda o app. Uma webcam USB serve como MVP.

## App mobile

Android nativo, em Kotlin.

### Como funciona

- **Iniciar corrida**: liga a câmera e o GPS. A cada frame, se o intervalo mínimo desde a última foto já passou, o frame é processado.
- **Uma foto por segundo**: mesmo a câmera entregando frames mais rápido, o app descarta os frames intermediários e só processa um a cada 1000 ms.
- **Filtro de obstrução**: cada frame candidato passa por dois testes antes de virar foto salva:
  - **Objeto na frente** (carro, placa de pare, semáforo): detectado com um modelo `EfficientDet-Lite0` via MediaPipe Tasks Vision; descarta se o objeto ocupar mais de 4% da imagem.
  - **Túnel / baixa luminosidade**: descarta se a luminosidade média do frame estiver abaixo de um limiar.
- **Armazenamento local**: fotos aprovadas são salvas no armazenamento interno do app e registradas num banco Room (`vegetacao.db`), junto com a coordenada GPS e o horário.
- **Envio**: um `WorkManager` reenvia periodicamente as fotos pendentes para a API do dashboard assim que houver internet, e apaga o arquivo local após confirmação de envio.

### Estrutura

| Arquivo | Responsabilidade |
|---|---|
| `MainActivity.kt` | Tela (Compose), permissões, câmera (CameraX) e GPS |
| `Corrida.kt` | Processa cada frame: limite de 1 foto/s, filtro e salvamento |
| `Filtro.kt` | Detecção de obstrução (objeto na frente / baixa luz) |
| `Banco.kt` | Entidade e DAO do Room (`Captura`, `CapturaDao`) |
| `Envio.kt` | `WorkManager` que envia fotos pendentes para a API |

### Como rodar

1. Abra a pasta raiz do projeto no Android Studio.
2. Baixe o modelo [`efficientdet_lite0.tflite`](https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/float32/latest/efficientdet_lite0.tflite) e coloque em `app/src/main/assets/`.
3. No `local.properties` (não versionado), defina `api.url=http://IP_DO_PC:8000/api/capturas` e, se o servidor usar chave, `api.token=SUA_CHAVE`. Sem `api.url`, o padrão é `http://10.0.2.2:8000/api/capturas` (emulador).
4. Conecte um celular Android via USB com depuração USB ativada (recomendado — a câmera e o GPS reais não são simulados corretamente em emulador).
5. Rode pelo botão de play do Android Studio.

### Limitações conhecidas

- O detector reconhece apenas as categorias do dataset COCO (carro, ônibus, caminhão, moto, placa de pare, semáforo). Placas de trânsito em geral exigem um modelo próprio.
- Testado com a câmera do próprio celular; a integração com webcam USB (para o MVP de hardware) ainda depende de uma biblioteca UVC (ex.: AndroidUSBCamera).
- O app não roda em segundo plano — mantém a tela ligada durante a corrida, mas não é um foreground service.

## Licença

Projeto acadêmico — FIAP.
