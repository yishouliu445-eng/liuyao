import HomePage from '../pages/HomePage';
import EntryModeTabs from '../components/input/EntryModeTabs';
import AskedMatterForm from '../components/input/AskedMatterForm';
import ManualDivinationForm from '../components/input/ManualDivinationForm';
import DirectionConfirmationDialog from '../components/input/DirectionConfirmationDialog';
import SimulatedCoinCasting, {
  buildCoinCastingPayload,
  type CoinCastResult,
  type SimulatedCoinCastingProps,
} from '../components/input/SimulatedCoinCasting';

void HomePage;
void EntryModeTabs;
void AskedMatterForm;
void ManualDivinationForm;
void DirectionConfirmationDialog;
void SimulatedCoinCasting;

const sampleCast: CoinCastResult = {
  id: 'cast-1',
  coins: ['yang', 'yin', 'yang'],
  line: '少阴',
  moving: false,
};

const castingProps: SimulatedCoinCastingProps = {
  casts: [sampleCast],
  isCasting: false,
  submitting: false,
  onCast: () => undefined,
  onReset: () => undefined,
};

const castingPayload = buildCoinCastingPayload([sampleCast]);

void castingProps;
void castingPayload;
