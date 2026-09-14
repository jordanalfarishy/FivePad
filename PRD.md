# FivePad — Product Requirements Document

Lima slot catatan, satu daftar tugas, tersinkron antara macOS dan Android — dan selalu satu klik jauhnya dari menu bar.

| | |
|---|---|
| **Versi** | 1.0 |
| **Tanggal** | 11 September 2026 |
| **Status** | Draft |
| **Platform** | macOS 13+ · Android 8+ |
| **Target rilis** | M1–M5 · ±19 minggu |

> Versi web dokumen ini (dengan filter platform pada §8): https://claude.ai/code/artifact/ba54b8fd-ac3e-40fe-aad4-823538eac8c0

> **Pembaruan implementasi Android Notes — 14 September 2026:** audit perbandingan FiveNotes Mobile dan rincian implementasi terbaru tersedia di [Mobile Notes feature audit](docs/mobile-notes-feature-audit.md). Penyimpanan catatan kini langsung masuk antrean berurutan, menggantikan debounce 400 ms; slot terakhir dan mode Markdown tersimpan. Aksi tempel/bagikan/ekspor, riwayat pemulihan, cadangan catatan lokal, impor dengan pratinjau, penerimaan teks berbagi, widget catatan, dan ubin Pengaturan Cepat telah ditambahkan. Sinkronisasi serta ekspor/impor tugas tetap fase berikutnya.


---

## Daftar isi

1. [Ringkasan eksekutif](#1-ringkasan-eksekutif)
2. [Latar belakang & masalah](#2-latar-belakang--masalah)
3. [Tujuan & non-tujuan](#3-tujuan--non-tujuan)
4. [Pengguna sasaran](#4-pengguna-sasaran)
5. [Lanskap kompetitif](#5-lanskap-kompetitif)
6. [Prinsip produk](#6-prinsip-produk)
7. [Cakupan produk](#7-cakupan-produk)
8. [Kebutuhan fungsional](#8-kebutuhan-fungsional)
9. [Model data](#9-model-data)
10. [Arsitektur sinkronisasi](#10-arsitektur-sinkronisasi)
11. [Kebutuhan non-fungsional](#11-kebutuhan-non-fungsional)
12. [Keputusan teknologi](#12-keputusan-teknologi)
13. [Alur & layar utama](#13-alur--layar-utama)
14. [Rencana rilis](#14-rencana-rilis)
15. [Metrik keberhasilan](#15-metrik-keberhasilan)
16. [Risiko & mitigasi](#16-risiko--mitigasi)
17. [Pertanyaan terbuka](#17-pertanyaan-terbuka)
18. [Di luar cakupan](#18-di-luar-cakupan)
19. [Glosarium](#19-glosarium)

---

## 1. Ringkasan eksekutif

FivePad adalah aplikasi catatan cepat yang sengaja dibatasi: tepat lima slot catatan yang tidak bisa ditambah, ditambah satu daftar tugas global. Batasan itu bukan kekurangan — justru itulah produknya.

Aplikasi catatan pada umumnya kalah oleh keberhasilannya sendiri. Setelah ratusan catatan menumpuk, membuka aplikasi berarti menghadapi daftar, folder, dan tag — bukan menulis. FivePad menghapus seluruh lapisan itu: yang ada hanya lima titik berwarna. Catatan keenam berarti kamu harus memutuskan mana dari lima yang sudah tidak penting.

Pembeda utamanya dari produk sejenis adalah jangkauan platform. Aplikasi minimalis sekelas ini nyaris seluruhnya eksklusif ekosistem Apple karena bersandar pada iCloud. FivePad menargetkan **macOS dan Android sekaligus** dengan lapisan sinkronisasi sendiri, sehingga melayani kelompok yang selama ini tidak terlayani: pengguna Mac yang memakai ponsel Android.

Di macOS, titik masuk utamanya bukan jendela aplikasi melainkan **ikon menu bar**: satu pintasan keyboard memunculkan panel untuk membaca dan mengedit catatan tanpa berpindah aplikasi, lalu hilang lagi. Target waktu dari tekan tombol sampai kursor siap mengetik adalah 120 milidetik.

---

## 2. Latar belakang & masalah

Ada tiga masalah yang saling menguatkan pada alat catatan cepat saat ini.

**01 — Gesekan menaruh pikiran.** Untuk mencatat satu kalimat, pengguna harus berpindah aplikasi, membuat catatan baru, memberi judul, lalu kembali. Biaya sepuluh detik itu cukup untuk membuat pikiran tersebut tidak jadi dicatat sama sekali.

**02 — Penumpukan tanpa batas.** Aplikasi tanpa batas jumlah catatan berubah jadi tempat pembuangan. Tanpa paksaan untuk memangkas, isinya menjadi arsip yang tidak pernah dibuka, bukan ruang kerja aktif.

**03 — Terkunci ekosistem.** Alat minimalis terbaik di macOS memakai iCloud, sehingga tidak ada jalan ke Android. Pengguna Mac + Android terpaksa memilih aplikasi lintas platform yang berat, atau hidup tanpa sinkronisasi.

---

## 3. Tujuan & non-tujuan

### Tujuan

- Menangkap pikiran dalam bawah 2 detik sejak niat muncul, di kedua platform.
- Menjaga lima slot dan daftar tugas tetap identik di semua perangkat tanpa campur tangan pengguna.
- Berfungsi penuh saat offline; sinkronisasi adalah bonus, bukan syarat.
- Menjadikan menu bar macOS sebagai permukaan utama, bukan pelengkap.
- Tidak pernah kehilangan tulisan pengguna — termasuk saat aplikasi ditutup paksa.
- Menyimpan data sesedikit mungkin di server dan tidak mengumpulkan telemetri.

### Non-tujuan

- Bukan pengganti Notion, Obsidian, atau aplikasi manajemen pengetahuan.
- Tidak ada folder, tag, backlink, atau hierarki apa pun.
- Tidak ada kolaborasi, berbagi, atau komentar antarpengguna.
- Tidak ada lampiran gambar, berkas, atau media pada fase awal.
- Tidak mengejar kelengkapan fitur; setiap penambahan harus melewati §6.
- Tidak ada fitur AI pada v1.

---

## 4. Pengguna sasaran

| Persona | Konteks | Kebutuhan yang dilayani |
|---|---|---|
| **Developer lintas ekosistem** | Bekerja di MacBook, memakai ponsel Android. Sepanjang hari berpindah antara editor, terminal, dan browser. | Tempat menaruh potongan perintah, URL, dan catatan rapat tanpa meninggalkan jendela yang sedang aktif. Isinya harus ada di ponsel saat meninggalkan meja. |
| **Pekerja lepas & konsultan** | Menangani tiga sampai lima klien aktif sekaligus. | Satu slot per klien memberi struktur tanpa perlu mengelola folder. Daftar tugas global menjawab "apa yang harus saya kerjakan sekarang". |
| **Mahasiswa** | Laptop di kelas, ponsel di perjalanan. Sensitif terhadap harga. | Catatan kuliah dan daftar tugas yang berpindah sendiri antara laptop dan ponsel. Jenjang gratis harus benar-benar berguna. |

> **Anti-persona.** Orang yang ingin memindahkan seluruh arsip catatannya ke FivePad bukan target kita. Menolak kelompok ini secara sadar adalah bagian dari strategi produk — melayani mereka berarti membongkar batasan lima slot yang menjadi inti nilai jualnya.

---

## 5. Lanskap kompetitif

| Produk | Platform | Harga | Celah yang kita isi |
|---|---|---|---|
| **FiveNotes** (Apptorium) | macOS, iOS, watchOS | $7,99 sekali bayar | Referensi konsep terdekat dan eksekusinya rapi, tapi terkunci di iCloud. Tidak ada jalur ke Android sama sekali, dan todolist bukan warga kelas satu. |
| **DoteNote** | Android | Gratis | Konsep lima slot berwarna yang nyaris identik — geser untuk berpindah, autosave, Material You — tapi hanya hidup di Android. Tidak ada aplikasi desktop, tidak ada menu bar, dan tidak ada jalan ke Mac. |
| **Apple Notes** | Apple saja | Gratis | Tanpa batas catatan sehingga cepat menumpuk; tidak ada akses menu bar; tidak ada Android. |
| **Google Keep** | Web, Android, iOS | Gratis | Lintas platform dan kuat di Android, tapi aplikasi Mac-nya hanya web. Tidak ada menu bar, tidak ada Markdown, dan modelnya kartu tak terbatas. |
| **Obsidian / Notion** | Semua | Gratis–$10/bln | Terlalu berat untuk menangkap satu kalimat. Waktu buka sampai siap mengetik dihitung dalam detik, bukan milidetik. |

> **Posisi.** FiveNotes menguasai sisi Apple, DoteNote menguasai sisi Android, dan tidak satu pun dari keduanya menjembatani celah di antaranya. FivePad adalah satu-satunya aplikasi catatan berbatas-lima yang menghadirkan lima slot yang *sama* di menu bar macOS dan di Android. Konsekuensinya tajam: sinkronisasi (M2) bukan sekadar jalur kritis — ia satu-satunya alasan produk ini berhak ada. Sebelum M2 selesai, FivePad hanyalah aplikasi lima-slot ketiga di pasar yang sudah punya dua.

---

## 6. Prinsip produk

Setiap usulan fitur diuji terhadap keempat prinsip ini. Usulan yang melanggar salah satunya ditolak, sekalipun banyak yang meminta.

**P1 — Lima adalah lima.** Jumlah slot tidak pernah bertambah. Aturan ini ditegakkan di basis data lewat `check (slot between 1 and 5)`, bukan hanya di antarmuka, supaya tidak bisa dilanggar secara diam-diam lewat jalur mana pun.

> **Pengecualian yang disengaja: grup tugas.** §18 menolak hierarki, dan Q3 semula memutuskan satu daftar tugas datar. FR-2.14 melonggarkannya untuk daftar tugas saja, atas permintaan eksplisit pemilik produk. Alasan pengecualian ini dapat dipertahankan: batasan lima slot melindungi *catatan* dari penumpukan, dan itu tetap utuh — jumlah slot tidak berubah. Daftar tugas punya bentuk masalah yang berbeda; dua puluh tugas tanpa pembagian lebih sulit dibaca ketimbang dua puluh tugas dalam empat grup. Yang tetap dijaga: grup hanya **satu tingkat**, karena begitu grup boleh berisi grup, kita sudah membangun folder lewat pintu belakang.

**P2 — Tanpa tombol simpan.** Pengguna tidak pernah diminta menyimpan, memberi judul, atau memilih lokasi. Mengetik sudah berarti menyimpan.

**P3 — Offline adalah keadaan normal.** Setiap fitur dirancang untuk berjalan tanpa jaringan. Sinkronisasi adalah proses yang terjadi di latar belakang, bukan syarat sebelum pengguna boleh bekerja.

**P4 — Data pengguna bukan produk kita.** Tanpa iklan, tanpa analitik pihak ketiga, tanpa pelatihan model. Isi catatan hanya disentuh untuk disinkronkan dan dicadangkan.

---

## 7. Cakupan produk

### Palet lima slot

Kelima slot dibedakan hanya oleh warna dan label. Warna bersifat tetap dan tidak dapat diubah pengguna — konsistensinya yang membuat pengguna hafal "yang hijau itu urusan klien A" tanpa perlu membaca label.

**Aksen** — satu nilai per slot per tema. Bukan versi terang dan gelap dari warna yang sama: di atas latar terang, aksen tema gelap jatuh di bawah 2:1, jadi kelimanya dihitung ulang.

| Slot | Gelap | Kontras | Terang | Kontras |
|---|---|---|---|---|
| Slot 1 | `#EF7A5A` | 5,68:1 | `#DB2F00` | 4,51:1 |
| Slot 2 | `#E0A63F` | 7,25:1 | `#A06700` | 4,50:1 |
| Slot 3 | `#63BC85` | 6,78:1 | `#1A8442` | 4,51:1 |
| Slot 4 | `#48BEDD` | 7,24:1 | `#0E7D9B` | 4,51:1 |
| Slot 5 | `#A186D6` | 5,16:1 | `#5320B7` | 8,83:1 |

Kontras diukur terhadap chrome, permukaan tempat nama catatan berdiri. Nilai terang di Figma — `#E73200` `#E49200` `#1B8744` `#13A4CB` — hanya mencapai **2,37–4,34:1**: empat dari lima gagal AA sebagai teks. Masing-masing digelapkan pada hue yang sama sampai tepat menyentuh 4,5:1; yang kelima sudah lolos dan tidak disentuh. **Satu nilai per slot, bukan dua**, supaya titik, pita, judul, dan pil tetap satu warna — itulah yang membuat kedua ujung layar menjawab "slot mana" bersama-sama.

**Permukaan** — kedua tab memakai permukaan yang sama, dan setiap nilai punya pasangan terangnya. Tidak ada latar selayar penuh per slot.

| Peran | Gelap | Terang |
|---|---|---|
| Latar isi | `#19191B` | `#EAEAE8` |
| Chrome (bilah status, bilah atas, baris nama, bilah bawah) | `#232324` | `#F9F9F9` |
| Kartu baris (tugas, pengaturan, lembar) | `#242525` | `#FFFFFF` |
| Pita pemisah | `#131314` | `#DDDDDA` |
| Tinta | `#FFFFFF` | `#25242C` |
| Kotak centang kosong | `#48484B` / tepi `#6B6B6B` | `#EFEFED` / tepi `#D7D7D7` |

Sisanya diturunkan dari tinta dengan alpha yang sama di kedua tema — garis rambut 16%, teks redup 40%, tepi titik 24%, pegangan seret 10% — karena begitulah Figma menggambarnya: satu tinta dengan ketebalan berbeda, bukan warna-warna terpisah.

> **Mengapa latar selayar penuh dilepas.** Versi sebelumnya mengecat seluruh layar dengan warna slot. Itu menarik pada tangkapan layar pertama, tapi berarti lima permukaan berbeda yang setiap nilainya harus diverifikasi sendiri-sendiri, dan warna pekat selebar layar membuat teks catatan — yang justru isi utamanya — harus bersaing dengan latarnya. Keputusan pemilik produk: satu permukaan gelap, dan warna slot dipadatkan ke dua tempat kecil yang justru paling sering dilihat.

**Pembawa warna slot.** Tiga, semuanya di sepertiga atas layar:

| Elemen | Slot aktif | Slot tidak aktif |
|---|---|---|
| Titik penanda | Aksen penuh, lingkaran 24 dp, cincin tinta 2 dp **di luar** lingkaran | Aksen pada opasitas **0,40** |
| Nama catatan | Aksen penuh | — |
| Pita 4 dp (node 5:1523) | Aksen penuh, berpola (lihat bawah) | — |
| Pil tab Catatan di bilah bawah | Aksen penuh, dengan latar aksen 16% | — |

Pil navigasi menutup lingkarannya: warna yang sama muncul di tepi atas dan tepi bawah layar, jadi kedua ujungnya menjawab "slot mana" dengan satu warna. Tab Tugas memakai aksen aplikasi, bukan aksen slot — daftar tugas memang tidak milik slot mana pun.

Nama catatan memakai aksen slot penuh dan lolos AA pada kesepuluh kombinasi slot × tema — lihat tabel aksen di atas.

Pita duduk tepat di bawah nama catatan. Saat catatan digulir, **namanya ikut pergi tapi pitanya menempel di tepi atas** — nama slot hanya perlu dilihat sesekali, sedangkan penanda slot tidak boleh pernah hilang dari layar.

**Pola pita — akses buta warna.** Setiap slot punya pola isian sendiri, bukan hanya warna sendiri. Polanya mengikuti mode buta warna Trello, dan **tidak ada yang polos** — pola yang polos bukan pola, dan slot yang memakainya akan jadi satu-satunya yang kembali bergantung pada warna saja.

| Slot | Pola | Ukuran |
|---|---|---|
| Slot 1 | Kisi belah ketupat (silang tipis) | goresan 1,2 dp, jarak 9 dp |
| Slot 2 | Belah ketupat rapat (papan catur diputar 45°) | jarak 4 dp |
| Slot 3 | Goresan miring kanan 45° | isi 3 dp, jarak 8 dp |
| Slot 4 | Goresan tegak | isi 2 dp, jarak 5 dp |
| Slot 5 | Goresan miring kiri 45° | isi 3 dp, jarak 8 dp |

Pola dibentuk oleh **dua nada dari warna aksen yang sama** — aksen penuh sebagai dasar, dan aksen yang dicampur 38% putih sebagai goresan. Bukan aksen lawan celah kosong: celah kosong berarti latar hampir-hitam ikut jadi bagian pola, hasilnya pita yang terlihat rusak dan bertepi gerigi.

Sekitar satu dari dua belas pria mengalami buta warna merah-hijau; bagi mereka Slot 1 (oranye) dan Slot 3 (hijau) adalah dua rona lumpur yang nyaris sama. Sejak latar selayar penuh dilepas, warna adalah satu-satunya yang menjawab "saya di slot mana" — pola menjadikannya dua saluran, sesuai NFR-8.

Slot 1 dan Slot 2 adalah pasangan paling berisiko tertukar, karena keduanya berbasis belah ketupat. Yang memisahkannya dibuat dua lapis: **figur lawan dasar** (Slot 1 didominasi aksen dengan kisi tipis di atasnya, Slot 2 didominasi nada terang dengan segitiga aksen di sela-selanya) dan **skala** (9 dp lawan 4 dp). Satu lapis saja tidak cukup — pada pita setinggi 4 dp, dua pola yang hanya berbeda kerapatan akan terbaca sama saat dilihat sambil lalu.

Polanya selalu menyala tanpa sakelar — aksesibilitas di balik pengaturan adalah aksesibilitas yang tidak pernah ditemukan orang yang membutuhkannya, dan pada pita 4 dp biayanya bagi yang lain praktis nol.

Opasitas titik tidak aktif dinaikkan dari 0,24 (nilai Figma) ke **0,40** dan berhenti di sana. Titik yang tidak terpilih adalah *state* tidak aktif, yang dikecualikan WCAG 1.4.11; yang wajib teridentifikasi adalah yang terpilih, dan itu tampil beraksen penuh dengan cincin tinta 2 dp di luarnya — 12–15:1 terhadap chrome. Menaikkannya lebih jauh meratakan beda terpilih dan tidak terpilih: kerugian nyata demi kemenangan aksesibilitas yang semu. Pada 0,40 nilainya 1,7–2,3:1.

Isi catatan memakai tinta penuh: 17,6:1 di tema gelap, 12,8:1 di tema terang.

**Teks sekunder** — label seksi, penanda Markdown, teks tugas selesai, panah, dan `⋮` — memakai alpha yang berbeda per tema, karena tinta gelap yang diencerkan kehilangan kontras jauh lebih cepat daripada tinta putih. Figma memakai 0,40 di keduanya, yang memberi 3,81:1 di gelap dan hanya 2,30:1 di terang; keduanya gagal. Nilai yang dipakai adalah yang terendah yang mencapai 4,5:1 pada permukaan terlemah tema itu: **0,47 di tema gelap** (terukur 4,76:1 pada layar) dan **0,65 di tema terang** (terukur 4,52:1).

Placeholder nama catatan memakai aksen slot pada alpha yang sama. Sebagai petunjuk isian ia tetap di bawah ambang; yang diketik pengguna tampil beraksen penuh.

**Aksen aksi** — berbeda per tema, karena **satu merah tidak bisa lolos 4,5:1 di atas latar gelap dan latar terang sekaligus**: menaikkannya untuk yang satu menurunkannya untuk yang lain. Keduanya berada pada hue `#E6210F`, warna sudut terlipat pada ikon aplikasi, jadi warna tindakan dan warna merek tetap satu benda.

| Tema | Aksen | Latar | Kartu | Pil (aksen 16%) |
|---|---|---|---|---|
| Gelap | `#FF5242` | 5,47:1 | 4,78:1 | 4,01:1 |
| Terang | `#C71C0D` | 4,84:1 | 5,83:1 | 4,24:1 |

Dipakai untuk ikon tambah, kotak centang yang tercentang (tepi `#FF4332`), dan pil tab Tugas. **Tidak** dipakai untuk teks "New Group", yang justru diredupkan (FR-2.17), dan **tidak** untuk tombol terisi di layar kosong.

**Isian tombol layar kosong** punya nilai sendiri: `#E6210F`, sama di kedua tema, dengan ikon dan teks putih (node 11:147). Alasannya berbeda arah dari aksen di atas: aksen dipakai sebagai *teks di atas halaman*, jadi nilainya harus berbeda per tema agar kontras; di sini warnanya justru yang menjadi latar, dan yang harus kontras adalah putih di atasnya. `#E6210F` memberi **4,58:1** dengan putih — lolos AA di kedua tema dengan satu nilai. Lolos AA di setiap permukaan tempat ia menjadi teks, kecuali satu: **angka "n/m" di dalam pil tab Tugas** (4,0–4,2:1). Latar pil adalah aksen itu sendiri pada 16%, jadi teks dan latarnya sehue — batas struktural desain, bukan akibat pilihan warnanya. Memperbaikinya berarti salah satu dari dua: angkanya memakai tinta alih-alih aksen, atau pilnya diperdalam jadi keping terisi dengan teks putih. Keduanya mengubah desain, jadi menunggu keputusan pemilik desain. Pendahulunya `#304678` hanya 1,90:1.

### Modul & kepemilikan platform

| Modul | Nama | Ada di |
|---|---|---|
| `FR-1` | Lima slot catatan | macOS + Android |
| `FR-2` | Daftar tugas | macOS + Android |
| `FR-3` | Akun & sinkronisasi | macOS + Android + backend |
| `FR-4` | Menu bar & panel cepat | macOS saja |
| `FR-5` | Jendela utama macOS | macOS saja |
| `FR-6` | Aplikasi Android | Android saja |
| `FR-7` | Pengaturan & data | macOS + Android |

---

## 8. Kebutuhan fungsional

**P0** wajib ada untuk rilis pertama modul tersebut. **P1** direncanakan untuk v1.0 publik. **P2** boleh digeser ke rilis berikutnya tanpa memblokir peluncuran.

### FR-1 — Lima slot catatan

| ID | Prio | Platform | Kebutuhan |
|---|---|---|---|
| FR-1.1 | P0 | Semua | Aplikasi menyediakan tepat lima slot catatan permanen. Slot tidak dapat ditambah maupun dihapus, dan jumlahnya tidak dapat diubah lewat jalur mana pun termasuk impor. |
| FR-1.2 | P0 | Semua | Setiap slot memiliki label yang dapat diubah pengguna, maksimal 24 karakter, dan satu warna tetap dari palet lima warna di §7. |
| FR-1.3 | P0 | Semua | Isi catatan berupa teks polos dengan konvensi Markdown. Batas 50.000 karakter per slot; saat mendekati batas, tampilkan peringatan pada 45.000 karakter. |
| FR-1.4 | P0 | Semua | Perubahan disimpan otomatis ke penyimpanan lokal 400 ms setelah pengguna berhenti mengetik, dan segera saat editor kehilangan fokus, aplikasi masuk ke background, atau panel ditutup. |
| FR-1.5 | P0 | macOS | Berpindah slot lewat klik pada titik warna atau pintasan `⌘1`–`⌘5`. `⌘\` kembali ke slot yang sebelumnya aktif. |
| FR-1.6 | P0 | Android | Berpindah slot lewat tap pada titik warna atau geser horizontal pada area editor. |
| FR-1.7 | P1 | Semua | Markdown dirender langsung saat mengetik untuk: tebal, miring, judul H1–H3, daftar berpoin, daftar bernomor, kotak centang, tautan, kode sebaris, blok kode, dan kutipan. |
| FR-1.14 | P1 | Semua | **Dua cara memandang isi yang sama.** *Teks biasa* menata isinya dan **menyembunyikan penandanya**: judul tampil besar, `- [ ]` menjadi kotak centang bundar, kutipan dan blok kode berlatar penuh selebar kolom dengan pita aksen slot di tepi kiri. *Markdown* menampilkan berkas sumbernya apa adanya dalam huruf lebar tetap. Pilihannya berlaku untuk kelima slot sekaligus — ia menyangkut cara membaca, bukan isi catatannya — dan tidak mengubah satu karakter pun yang tersimpan. |
| FR-1.15 | P1 | Semua | Menu penyuntingan teks berisi sebelas tindakan: Header, Sub header, Bold, Italic, Strike, List, Ordered List, To do, Quote, Code, Link — ditambah sakelar tampilan FR-1.14 di baris paling atas. Di Android diakses lewat ikon `match_case` di bilah atas dan tampil sebagai **petak empat kolom**, bukan daftar bertumpuk: sebelas baris menghasilkan lembar setinggi hampir satu layar, dan lembar setinggi itu menutupi justru catatan yang sedang diformat. Di macOS lewat menu utama dengan pintasan papan tik. Setiap tindakan bersifat **membalik** — menerapkannya pada teks yang sudah memakainya justru mencabutnya, karena di tampilan teks biasa penandanya bahkan tidak terlihat untuk dihapus manual.  Petaknya dikelompokkan menurut **apa yang tersentuh**: satu baris penuh (judul; lalu daftar; lalu blok) atau sepotong teks yang dipilih (penekanan). Kelompoknya tidak berlabel — jaraknya sudah mengatakan hal yang sama tanpa memakan tinggi lembar — dan kelompok yang melebihi empat petak digulung ke samping, bukan dibungkus ke bawah, supaya tinggi lembarnya tidak ikut tumbuh saat tindakan bertambah. Sakelar tampilan FR-1.14 duduk sebaris dengan judul lembar, bukan menjadi salah satu petak: ia mengubah cara seluruh catatan dibaca. |
| FR-1.15a | P1 | Semua | Coret memakai `~~coret~~`, bentuk yang dikenali aplikasi lain. Satu tilde tidak dianggap penanda apa pun: dua bentuk untuk satu arti berarti catatan yang tampil berbeda di aplikasi lain daripada di sini. **Gaya boleh dipadukan.** Teks biasa, daftar, daftar bernomor, dan tugas menerima Bold, Italic, Strike, Code, dan Link; judul dan subjudul menerima Italic dan Strike; kutipan menerima Sub header, teks biasa, Bold, Italic, Strike, Code, dan Link. Penanda blok selalu ditulis lebih dulu — `> ## Judul`, bukan `## > Judul` — karena hanya urutan itu yang berarti "subjudul di dalam kutipan". |
| FR-1.16 | P1 | Semua | **Enter melanjutkan daftar.** Menekan Enter di dalam daftar, daftar bernomor, atau daftar tugas membuat butir berikutnya dengan penanda yang sama; nomor bertambah satu dan tugas baru selalu lahir belum tercentang. Menekan Enter pada butir yang masih kosong justru **mencabut** penandanya — itulah cara mengakhiri daftar, dan tanpa itu satu-satunya jalan keluar adalah menghapus penanda yang tidak terlihat. |
| FR-1.17 | P1 | Semua | **Tautan ditanyakan, bukan diketik di tempat.** Memilih Link membuka lembar berisi Teks dan Alamat; teks yang sedang terseleksi mengisi label lebih dulu, dan fokus mendarat di alamat. Alasannya teknis sekaligus praktis: di tampilan teks biasa `](alamat)` disembunyikan begitu polanya lengkap, jadi tautan yang disisipkan langsung ke catatan akan lenyap dari layar pada detik yang sama alamatnya harus diketik. Memilih Link di atas tautan yang sudah ada membongkarnya kembali menjadi labelnya. |
| FR-1.18 | P1 | Semua | **Tautan bisa dibuka.** Mengetuk label tautan di tampilan teks biasa membukanya di peramban; alamat tanpa skema dilengkapi `https://` lebih dulu. Yang dikonsumsi hanya angkat-jarinya, bukan turun-jarinya — gestur yang ternyata sebuah gulungan harus tetap menggulung, dan menahan turun-jari akan mematikan gulungan yang berawal di atas sebuah tautan. |
| FR-1.19 | P0 | Semua | **Layar mengikuti kursor.** Setiap kali kursor berpindah — diketik, dipindah, atau dibawa tindakan format — layar menyusul sampai kursornya terlihat, dengan sedikit napas di atas atau di bawahnya. Tanpa ini, menggulung menjauh lalu mengetik berarti mengetik ke tempat yang tidak terlihat: kolom teksnya tidak menggulung sendiri, yang menggulung adalah kolom di luarnya bersama baris judul.  **Berpindah slot menutup papan ketik** dan melepas fokus: pindah slot berarti selesai menulis di slot sebelumnya, dan papan ketik yang tertinggal terbuka menutupi separuh catatan yang baru dibuka sekaligus mengantar ketikan berikutnya ke slot yang salah. Membuka menu slot juga menutupnya — menyalin dan mengosongkan sama-sama menjawab lewat snackbar di tepi bawah layar, dan papan ketik persis menutupi tepi itu; "Urungkan" yang tidak terlihat sama saja dengan tidak ada. |
| FR-1.20 | P0 | Semua | **Teks yang sedang diketik milik editor, bukan milik draft.** Draft di ViewModel sampai lewat StateFlow dan selalu tertinggal satu bingkai; bila gema itu diperlakukan sebagai perubahan dari luar, ia menimpa huruf yang sudah diketik sesudahnya. Editor hanya menerima teks dari luar bila teks itu **berbeda dari yang terakhir ia kirim** — pengosongan slot, pengurungan, atau kelak sinkronisasi. Tanpa aturan ini huruf hilang saat mengetik cepat, dan pada papan ketik yang menulis sekata sekaligus, hilangnya bisa sekata penuh.  Aturan yang sama berlaku untuk **nama slot**, dan di sana akibatnya lebih kasar: namanya dulu dikirim langsung ke basis data, jadi beberapa nilai lama sempat beredar di jalan sekaligus — mengetik "judul" menghasilkan "udulj". Nama slot kini melewati draft yang sama seperti isi catatan. |
| FR-1.21 | P1 | Semua | **Penekanan berhenti di spasi.** Menerapkan Tebal, Miring, atau Coret tanpa menyeleksi apa pun lalu mengetik berarti mengetik di antara sepasang penanda; spasi atau Enter berikutnya memindahkan kursor ke luar penanda itu. Tanpa aturan ini penandanya tidak pernah ditutup dan seluruh sisa kalimat ikut menebal. Yang dimaksud orang hampir selalu satu kata; untuk lebih dari itu, seleksi dulu lalu terapkan. Kode sebaris dikecualikan — `kode seperti ini` justru kerap memuat spasi. |
| FR-1.22 | P1 | Semua | **Tebal dan miring bisa bersamaan**, ditulis `***begini***`. Menerapkan Miring pada teks yang sudah tebal menambahkan miring, bukan menukar tebal menjadi miring — sebutir bintang di kiri-kanan seleksi belum tentu penanda miring, bisa jadi separuh dari penanda tebal yang mengapitnya. |
| FR-1.8 | P1 | Semua | Kotak centang Markdown `- [ ]` dapat diklik atau di-tap untuk berubah status tanpa masuk ke mode edit teks. |
| FR-1.9 | P1 | Semua | Penghitung kata dan karakter **tidak** ditampilkan terus-menerus: desain Figma tidak memuatnya, dan satu baris tetap di kaki editor memakan ruang menulis di setiap layar. Yang tersisa adalah peringatan ambang batas FR-1.3, yang muncul hanya sejak 45.000 karakter. Sakelar untuk menampilkannya kembali menyusul bersama FR-7.1. |
| FR-1.10 | P1 | Semua | Aksi "Salin seluruh isi slot" menyalin teks mentah Markdown — bukan hasil rendernya — ke papan klip. Di Android, tindakan milik slot dibuka dengan menekan-lama titik slotnya: titiknya **adalah** slotnya, jadi bilah atas tidak perlu tombol tambahan dan tata letak desain tetap utuh. |
| FR-1.11 | P1 | Semua | Aksi "Kosongkan slot" meminta konfirmasi, lalu menyimpan isi lama ke `note_revisions` sebelum menghapusnya, dan menawarkan urungkan selama 5 detik. Penyimpanan revisi dan pengosongan berjalan dalam satu transaksi — kalau terpisah, ada celah waktu di mana isi sudah hilang tapi salinannya belum ada. |
| FR-1.12 | P2 | macOS | Menyeret teks dari aplikasi lain ke sebuah titik warna menambahkan teks itu ke akhir slot bersangkutan, dipisahkan satu baris kosong. |
| FR-1.13 | P2 | Semua | Pencarian teks di kelima slot sekaligus, dengan penanda jumlah hasil per slot dan sorotan pada kecocokan. |

### FR-2 — Daftar tugas

| ID | Prio | Platform | Kebutuhan |
|---|---|---|---|
| FR-2.1 | P0 | Semua | Satu daftar tugas global, terpisah dari kelima slot catatan dan tidak terikat pada salah satunya. |
| FR-2.2 | P0 | Semua | Menambah tugas lewat satu kolom masukan yang juga menanyakan jatuh tempo (FR-2.10) sekaligus. Menekan Enter menyimpan tugas dan mengosongkan kolom — beserta jatuh temponya — agar siap untuk entri berikutnya, tanpa menutup lembarnya. Maksimal 500 karakter per tugas. |
| FR-2.3 | P0 | Semua | Menandai tugas selesai atau belum lewat kotak centang, dengan perubahan tersimpan seketika. **Seluruh baris adalah sasarannya**, bukan kotak centangnya saja — kotak 20 dp jauh di bawah sasaran sentuh yang wajar, dan sisa baris di sebelahnya tidak dipakai apa pun. Menyunting tetap lewat geser ke kanan: ketukan yang membuka lembar sunting berarti setiap usaha mencentang yang meleset sedikit justru membuka lembar. |
| FR-2.4 | P0 | Semua | Menyunting tugas tanpa berpindah layar, lewat lembar bawah berisi teks **dan** jatuh tempo sekaligus — bukan dua langkah terpisah. Di Android jalannya **hanya geser ke kanan**: ketukan pada baris sengaja tidak melakukan apa pun, karena baris itu sudah memuat dua sasaran ketuk (kotak centang dan pegangan seret) dan baris yang juga menerima ketukan di mana saja membuat setiap usaha mencentang yang meleset sedikit justru membuka lembar sunting. Di macOS: klik ganda pada baris. Lembar sunting **tidak** memuat tindakan hapus: menghapus sudah punya jalannya sendiri (geser ke kiri), dan tindakan merusak yang punya dua pintu berarti dua peluang salah tekan untuk satu hal yang hanya bisa diurungkan selama lima detik. |
| FR-2.5 | P0 | Semua | Menghapus tugas: geser ke kiri di Android; tombol hapus yang muncul saat kursor di atas baris, atau menu klik kanan, di macOS. |
| FR-2.6 | P0 | Semua | Penghitung kemajuan "n/m" tampil pada tab Tugas itu sendiri, sehingga terlihat juga saat pengguna sedang berada di tab Catatan. Tidak ada baris kemajuan terpisah yang memakan tinggi daftar. |
| FR-2.7 | P1 | Semua | Menyusun ulang tugas dengan seret dan lepas lewat pegangan khusus di tepi kiri baris **atau tekan-lama di mana saja pada barisnya**. Pegangannya tetap ada sebagai penanda — yang berubah hanya luas daerah yang menerima. Tekan-lama tidak merebut geser mendatar: geser baru menjadi seretan setelah jarinya diam, dan sampai itu hapus dan sunting tetap memilikinya. Seretan yang sama memindahkan tugas **ke grup lain**: bagian tujuan ditentukan dari posisi jari, dan garis sisip menunjukkan tempat jatuhnya. Daftar ikut bergulir sendiri saat jari mendekati tepi, sehingga grup di luar layar tetap bisa dituju. Grup dan posisi ditulis dalam satu transaksi. |
| FR-2.8 | P1 | Semua | Tugas yang selesai otomatis turun ke bagian bawah daftar. Perilaku ini dapat dimatikan lewat Pengaturan. |
| FR-2.9 | P1 | Semua | Aksi "Bersihkan yang selesai" menghapus seluruh tugas berstatus selesai sekaligus, dengan opsi urungkan selama 5 detik. Barisnya hanya muncul saat ada yang bisa dibersihkan dan memakai bahasa visual yang sama dengan "New Task" — tidak ada tombol merusak yang menunggu di layar saat tidak ada gunanya. |
| FR-2.10 | P1 | Semua | Tanggal dan waktu jatuh tempo opsional per tugas. Tugas yang lewat jatuh tempo ditandai dengan warna semantik, bukan hanya teks. |
| FR-2.11 | P1 | Semua | Notifikasi lokal pada waktu jatuh tempo, dijadwalkan di perangkat sehingga tetap berjalan tanpa koneksi. Izin notifikasi diminta **setelah** tugasnya tersimpan, bukan saat tanggalnya dipilih: diminta lebih awal, dialog sistem menutupi lembar yang masih terbuka, dan tombol kembali yang dipakai menyingkirkan dialog itu ikut menutup lembarnya — suntingan yang belum disimpan hilang. Ditolak pun jatuh temponya tetap tersimpan; yang tidak ada hanya pengingatnya. |
| FR-2.12 | P2 | Semua | Batas 500 tugas aktif. Melewati batas itu, tugas selesai yang paling lama diarsipkan otomatis dan tidak lagi disinkronkan. |
| FR-2.13 | P2 | macOS | Menyeret satu baris teks ke ikon menu bar sambil menahan `⌥` menambahkannya sebagai tugas baru, bukan sebagai catatan. |
| FR-2.14 | P0 | Semua | Tugas dapat dikelompokkan ke dalam grup bernama yang dibuat pengguna. Grup hanya satu tingkat — grup tidak boleh berisi grup lain. Tugas berada di tepat satu grup, atau di luar grup mana pun. |
| FR-2.15 | P0 | Semua | Grup dapat dibuat, diubah namanya (maks 40 karakter), dan dihapus. Menghapus grup **tidak** menghapus tugas di dalamnya; tugas itu kembali menjadi tanpa grup. Penghapusan tugas tidak pernah menjadi efek samping yang tersembunyi. |
| FR-2.16 | P1 | Semua | Urutan grup dapat diatur pengguna dan ikut tersinkronisasi. Grup kosong tetap ditampilkan agar pengguna bisa mengisinya; hanya grup yang dihapus yang hilang. |
| FR-2.17 | P1 | Semua | **Mengelompokkan datang setelah ada yang dikelompokkan.** Selama daftar benar-benar kosong — tanpa tugas dan tanpa grup — layar hanya menampilkan satu kalimat dan satu tombol terisi "New Task" di tengah, dan "New Group" tidak ada sama sekali. Begitu ada isinya, "New Group" muncul di kaki daftar dalam keadaan **diredupkan**: ia muncul sekali saja di sana, sementara tombol tambah tugas muncul di baris judul setiap bagian, sebelum menu sunting grup; kalau keduanya sama-sama beraksen, yang di kaki justru lebih menarik mata karena ia sendirian. |

### FR-3 — Akun & sinkronisasi

| ID | Prio | Platform | Kebutuhan |
|---|---|---|---|
| FR-3.1 | P0 | Semua | Aplikasi berfungsi penuh tanpa akun dan tanpa koneksi internet. Akun hanya diperlukan untuk menyalakan sinkronisasi, dan tidak pernah diminta saat pertama membuka aplikasi. |
| FR-3.2 | P0 | Backend | Autentikasi lewat surel dengan tautan masuk sekali pakai (magic link) dan lewat surel dengan kata sandi. |
| FR-3.3 | P1 | Semua | Masuk dengan Google di kedua platform, dan Masuk dengan Apple di macOS. Masuk dengan Apple wajib ada jika aplikasi didistribusikan lewat Mac App Store dan sudah menawarkan login pihak ketiga lain. |
| FR-3.4 | P0 | Semua | Saat pengguna masuk untuk pertama kali di perangkat yang sudah memuat data lokal, tampilkan pilihan eksplisit: gabungkan keduanya, pakai data cloud, atau pakai data lokal. Tidak boleh ada penggabungan diam-diam. |
| FR-3.5 | P0 | Semua | Seluruh perubahan ditulis ke penyimpanan lokal lebih dulu, lalu masuk antrean kirim. Antarmuka tidak pernah menunggu jawaban server sebelum menampilkan hasil. |
| FR-3.6 | P0 | Semua | Perubahan dari perangkat lain diterima lewat langganan realtime saat aplikasi aktif, dan lewat penarikan penuh saat aplikasi dibuka atau kembali ke latar depan. |
| FR-3.7 | P0 | Backend | Penyelesaian konflik memakai *last-write-wins* per baris berdasarkan stempel waktu server, bukan stempel waktu perangkat. Rinciannya di §10. |
| FR-3.8 | P0 | Semua | Indikator status sinkronisasi dengan empat keadaan: tersinkron, sedang menyinkronkan, luring, dan gagal. Keadaan gagal menyertakan alasan dan tombol coba lagi. |
| FR-3.9 | P0 | Semua | Setiap perangkat terdaftar dengan pengenal, nama, dan platform. Daftar perangkat tampil di Pengaturan beserta opsi mencabut akses dari jarak jauh. |
| FR-3.10 | P0 | Backend | Row Level Security aktif di seluruh tabel, memastikan pengguna hanya dapat membaca dan menulis baris miliknya sendiri. Tidak ada jalur akses yang melewati RLS. |
| FR-3.11 | P1 | Semua | Riwayat versi menyimpan 10 revisi terakhir per slot selama 30 hari, dengan pratinjau dan aksi pulihkan. |
| FR-3.12 | P1 | Semua | Saat perangkat lain menimpa slot yang baru saja diedit di perangkat ini, tampilkan panel kecil "versi lain menimpa perubahanmu" dengan tautan langsung ke riwayat versi. |
| FR-3.13 | P2 | Semua | Keluar dari akun menawarkan pilihan menghapus atau mempertahankan data lokal, dengan bawaan mempertahankan. |

### FR-4 — Menu bar & panel cepat (macOS)

| ID | Prio | Kebutuhan |
|---|---|---|
| FR-4.1 | P0 | Ikon berada di menu bar sistem. Klik kiri membuka dan menutup panel cepat. |
| FR-4.2 | P0 | Panel memuat baris lima titik warna, editor slot aktif, dan tab daftar tugas. Ukuran awal 360 × 480 pt, dapat diubah pengguna dan diingat antar sesi. |
| FR-4.3 | P0 | Panel dan jendela utama berbagi satu sumber data. Mengetik di salah satunya langsung terlihat di yang lain tanpa perlu menutup atau menyegarkan. |
| FR-4.4 | P0 | Pintasan global membuka dan menutup panel. Bawaannya `⌥Space`, dapat diganti di Pengaturan, dengan deteksi bentrok terhadap pintasan sistem dan aplikasi peluncur yang umum dipakai. |
| FR-4.5 | P0 | Saat panel terbuka, kursor teks langsung berada di editor slot yang terakhir aktif. Pengguna dapat mengetik tanpa mengklik apa pun. |
| FR-4.6 | P0 | Menekan `Esc` atau mengklik di luar panel akan menutupnya. Perubahan sudah tersimpan sebelum panel hilang; tidak ada dialog konfirmasi. |
| FR-4.7 | P1 | Opsi "Tampilkan di posisi kursor" memunculkan panel di dekat penunjuk tetikus alih-alih di bawah ikon menu bar. |
| FR-4.8 | P1 | Opsi "Selalu di atas" melepas panel menjadi jendela mengambang yang bertahan di atas aplikasi lain, termasuk saat aplikasi lain berada dalam mode layar penuh. |
| FR-4.9 | P1 | Kolom tambah tugas cepat di kepala panel. Menekan Enter menambahkan tugas tanpa menutup panel dan tanpa memindahkan fokus dari kolom itu. |
| FR-4.10 | P1 | Menjatuhkan teks ke ikon menu bar menambahkannya ke slot yang terakhir aktif, tanpa membuka panel. |
| FR-4.11 | P1 | Opsi "Jalankan saat login", didaftarkan lewat layanan login modern, bukan item login warisan. |
| FR-4.12 | P2 | Opsi menyembunyikan ikon Dock sehingga aplikasi hidup sepenuhnya di menu bar. |
| FR-4.13 | P2 | Klik kanan pada ikon menu bar membuka menu ringkas berisi kelima slot, Pengaturan, dan Keluar. |

### FR-5 — Jendela utama (macOS)

| ID | Prio | Kebutuhan |
|---|---|---|
| FR-5.1 | P0 | Jendela utama menampilkan baris lima titik, editor, dan panel daftar tugas berdampingan. Ukuran minimum 480 × 420 pt. |
| FR-5.2 | P1 | Ukuran huruf editor dapat diatur antara 12 dan 24 pt, tersimpan per perangkat dan tidak ikut tersinkronisasi. |
| FR-5.3 | P1 | Mode fokus menyembunyikan daftar tugas dan baris titik, menyisakan editor saja. |
| FR-5.4 | P1 | Tema terang dan gelap, mengikuti sistem secara bawaan, dengan opsi mengunci ke salah satunya. |

### FR-6 — Aplikasi Android

| ID | Prio | Kebutuhan |
|---|---|---|
| FR-6.1 | P0 | Layar tunggal: baris lima titik di tepi atas, editor di tengah, dan tab Catatan/Tugas di tepi bawah dalam jangkauan ibu jari. Permukaannya satu warna gelap untuk kedua tab; slot aktif ditandai oleh titiknya (aksen penuh + cincin putih 2 dp, sisanya opasitas 0,24), oleh nama catatan yang mengambil warna titik itu, dan oleh pita 4 dp berpola di bawahnya yang menempel di tepi atas saat digulir. Nama slot **ikut menggulung bersama isinya**, tidak terpaku di bilah atas: di layar ponsel setiap baris yang dipaku memakan ruang menulis, sementara nama slot hanya perlu dilihat sesekali. Mengetuk titik dari tab Tugas langsung kembali ke slot tersebut. Tidak ada laci navigasi maupun bilah bawah bertingkat.  Papan ketik **menutupi** bilah tab, tidak mendorongnya ke atas: mendorongnya memakan tinggi layar dua kali — sekali oleh papan ketiknya, sekali lagi oleh bilah yang ikut naik — padahal yang sedang dibutuhkan justru ruang menulis.  Sisipan papan ketik untuk isi dihitung sebagai selisihnya terhadap tinggi bilah itu, bukan setinggi papan ketik penuh: isinya sudah berhenti di atas bilah, jadi menyisipkannya penuh akan menghitung tinggi bilah dua kali dan meninggalkan pita kosong di antara isi dan papan ketik. |
| FR-6.2 | P0 | Papan ketik muncul otomatis saat aplikasi dibuka dari widget atau ubin Pengaturan Cepat, tapi tidak saat dibuka dari peluncur. Pembedanya datang dari niat yang membuka: `ACTION_MAIN` dari peluncur tidak membawa data maupun extra, jadi ia jatuh ke perilaku "hanya tampil" tanpa perlu diperiksa khusus. Tautan `fivepad://slot/{1..5}` membuka slot itu dengan papan ketik aktif (`?focus=0` untuk membukanya tanpa papan ketik); aktivitasnya `singleTask` supaya niat kedua mendarat di instans yang sudah berjalan. |
| FR-6.3 | P1 | Widget layar utama ukuran 2×2 dan 4×2 menampilkan satu slot pilihan atau daftar tugas. Menyentuh widget membuka langsung ke isi tersebut. |
| FR-6.4 | P1 | Menerima teks dari aplikasi lain lewat lembar berbagi sistem, dengan pemilih slot tujuan di dalam dialog berbagi. |
| FR-6.5 | P1 | Ubin Pengaturan Cepat membuka slot yang terakhir aktif dengan papan ketik langsung aktif. |
| FR-6.6 | P2 | Dukungan warna dinamis Material You sebagai tema opsional, dengan palet lima slot tetap tidak berubah agar identitas warna terjaga. |
| FR-6.7 | P0 | Dua tema, gelap dan terang, dengan **gelap sebagai bawaan**. Pilihannya milik aplikasi, bukan mengikuti sistem: FivePad dipakai sebagai papan tulis yang selalu terbuka, dan tema yang berubah sendiri mengikuti jadwal malam perangkat berarti latar yang berganti di tengah menulis. Pilihannya tersimpan dan dibaca sinkron saat aplikasi dibuka, sehingga tidak ada kedipan tema di bingkai pertama. **Layar pembuka ikut temanya** sejak Android 12: jendela awal digambar sistem sebelum satu baris kode aplikasi pun berjalan, jadi ia tidak bisa diberi tahu pilihan pengguna — yang bisa adalah sebaliknya, menitipkan pilihan itu ke sistem lewat `UiModeManager.setApplicationNightMode`, yang lalu menyelesaikan sumber daya aplikasi ini (termasuk tema pembukanya) dalam mode yang sama. Penitipannya dilakukan saat layar ditinggalkan, bukan seketika: seketika berarti aktivitas dibuat ulang di depan mata, padahal warnanya sudah berganti tanpa itu. Di Android 8–11 layar pembuka hanya bisa mengikuti mode perangkat.  **Ikon peluncur tidak ikut tema.** Berkas desainnya ada dua — plat gelap dan plat amber — tapi diukur di perangkat: peluncur menyelesaikan ikon aplikasi lain dengan konfigurasi tetap, jadi sumber daya bertanda `-night` tidak ikut mode gelap (diuji dengan mengganti mode lalu memulai ulang peluncur; platnya tidak berubah). Yang terpasang di layar utama adalah plat amber, dan alasannya bukan sekadar "itu bucket bawaan": ikon duduk di atas wallpaper orang, dan plat gelap lebur ke wallpaper gelap — keadaan yang paling umum. Jawaban Android untuk "ikon mengikuti tema" adalah lapisan monokrom (Android 13+), dan lapisan itu sudah ada. |

### FR-7 — Pengaturan & data

| ID | Prio | Platform | Kebutuhan |
|---|---|---|---|
| FR-7.1 | P0 | Semua | Halaman Pengaturan memakai bahasa visual yang sama dengan daftar tugas: seksi berjudul, kartu baris, pita pemisah. Bagian **Pengaturan Aplikasi** memuat Tema dan Bahasa; bagian **Versi Aplikasi** memuat Cek Pembaruan — yang mengantar ke halaman aplikasi di toko, karena pembaruan memang urusan toko dan tidak akan pernah jadi urusan aplikasi — lalu Syarat & Ketentuan dan Kebijakan Privasi, keduanya membuka halaman web. Di kaki halaman ada ajakan masuk akun beserta tombolnya; halaman masuknya sendiri datang di M2, dan sampai itu tombolnya menjawab "belum, tapi nanti" ketimbang diam. Tombol masuk **tidak** ditaruh di bilah atas layar utama: bilah itu sudah penuh dan tempat kanan-atasnya dipakai opsi teks (FR-1.15). Bahasa membuka pemilih bahasa per-aplikasi milik sistem (Android 13+), bukan pemilih buatan sendiri — pilihannya tersimpan di sistem dan tidak boleh ada dua tempat yang bisa berbeda jawaban. Akun, Sinkronisasi, dan Notifikasi menyusul bersama FR-3 di M2. |
| FR-7.2 | P1 | Semua | Ekspor ke berkas Markdown — lima berkas slot ditambah satu berkas daftar tugas — dan ke satu berkas JSON gabungan. |
| FR-7.3 | P1 | Semua | Impor dari berkas JSON hasil ekspor, dengan pratinjau perubahan sebelum ditimpa dan opsi batal. |
| FR-7.4 | P1 | Semua | Pencadangan lokal otomatis setiap hari, menyimpan tujuh salinan terakhir secara bergilir. |
| FR-7.5 | P2 | macOS | Aksi Apple Shortcuts untuk membaca slot, menambahkan teks ke slot, dan menambah tugas; ditambah skema URL `fivepad://` untuk otomatisasi dari aplikasi lain. |

---

## 9. Model data

Skema yang sama dipakai di Postgres (server) dan di basis data lokal masing-masing platform. Batasan lima slot ditegakkan di lapisan basis data, sesuai prinsip P1.

```
profiles
  id              uuid pk        -- sama dengan auth.uid()
  display_name    text
  created_at      timestamptz

notes
  id              uuid pk
  user_id         uuid fk → profiles.id
  slot            smallint       -- check (slot between 1 and 5)
  label           text           -- maks 24 karakter
  color           smallint       -- indeks palet, 1..5
  body            text           -- maks 50.000 karakter
  updated_at      timestamptz    -- waktu server, otoritatif
  client_updated_at timestamptz  -- waktu perangkat, untuk diagnosis
  device_id       uuid
  unique (user_id, slot)

note_revisions
  id              uuid pk
  note_id         uuid fk → notes.id
  body            text
  created_at      timestamptz    -- dipangkas: 10 terakhir / 30 hari

todos
  id              uuid pk
  user_id         uuid fk → profiles.id
  text            text           -- maks 500 karakter
  done            boolean
  due_at          timestamptz    -- nullable
  position        numeric        -- urutan pecahan, hindari re-index massal
  updated_at      timestamptz
  client_updated_at timestamptz
  device_id       uuid
  deleted_at      timestamptz    -- soft delete, dipanen setelah 30 hari

devices
  id              uuid pk
  user_id         uuid fk → profiles.id
  name            text           -- "MacBook Pro Jordan"
  platform        text           -- 'macos' | 'android'
  last_seen_at    timestamptz
  revoked_at      timestamptz
```

> **Catatan implementasi.** Penghapusan tugas memakai *soft delete*, bukan penghapusan langsung. Tanpa itu, perangkat yang lama luring akan menganggap baris yang hilang sebagai baris baru dan menghidupkannya kembali saat menyinkron — bug klasik pada sistem sinkronisasi dua arah.

> **Catatan implementasi.** Kolom `position` memakai angka pecahan, sehingga menyisipkan tugas di antara dua tugas lain cukup dengan mengambil nilai tengahnya. Ini menghindari penulisan ulang seluruh daftar setiap kali pengguna menggeser satu baris.

---

## 10. Arsitektur sinkronisasi

Sinkronisasi berjalan sebagai siklus empat langkah yang dipicu saat aplikasi dibuka, saat kembali ke latar depan, saat koneksi pulih, dan setiap kali antrean lokal berisi perubahan yang belum terkirim.

1. **Tulis lokal** — Setiap perubahan langsung masuk ke basis data lokal dan ditandai *dirty*. Antarmuka menampilkan hasilnya seketika tanpa menunggu server.
2. **Dorong** — Baris bertanda dirty dikirim ke server. Server menetapkan `updated_at` dari jamnya sendiri dan mengembalikan nilai itu, sehingga selisih jam antar perangkat tidak pernah memengaruhi hasil konflik.
3. **Tarik** — Klien meminta seluruh baris dengan `updated_at` lebih baru dari penanda sinkronisasi terakhirnya, lalu menyimpan penanda baru dari respons server.
4. **Dengarkan** — Selama aplikasi aktif, klien berlangganan kanal realtime untuk barisnya sendiri. Perubahan dari perangkat lain tiba tanpa perlu menarik ulang.

### Aturan penyelesaian konflik

| Situasi | Perilaku |
|---|---|
| **Dua perangkat mengedit slot yang sama** | Baris dengan `updated_at` server paling baru menang secara utuh. Versi yang kalah tetap tersimpan di `note_revisions` dan pengguna diberi tahu lewat FR-3.12. |
| **Dua perangkat mengubah tugas yang sama** | Sama, last-write-wins per baris. Karena satu tugas hanya berisi teks pendek dan satu status, kehilangan akibat konflik bernilai kecil. |
| **Satu perangkat menghapus, satu mengedit** | Penghapusan menang jika `deleted_at` lebih baru dari `updated_at` hasil edit. Selama 30 hari tugas masih bisa dipulihkan dari cadangan lokal. |
| **Perangkat luring lama kembali daring** | Dorong dulu, baru tarik. Perubahan luring yang lebih tua otomatis kalah, dan seluruh isi lokalnya dicadangkan sebelum siklus dimulai. |

> **Mengapa bukan CRDT.** CRDT seperti Yjs atau Automerge menyelesaikan penggabungan teks per karakter dan akan menghilangkan kehilangan data akibat konflik sepenuhnya. Biayanya adalah kompleksitas, ukuran aplikasi, dan riwayat operasi yang terus tumbuh. Untuk lima catatan milik satu orang, bentrokan sungguhan jarang terjadi — pengguna tunggal biasanya hanya mengetik di satu perangkat pada satu waktu. Last-write-wins ditambah riwayat versi sudah cukup untuk v1. Naik ke CRDT ditinjau ulang jika data lapangan menunjukkan tingkat konflik melebihi target di §15.

---

## 11. Kebutuhan non-fungsional

| ID | Aspek | Target terukur |
|---|---|---|
| NFR-1 | **Waktu buka** | Panel menu bar siap menerima ketikan ≤ 120 ms sejak pintasan ditekan. Aplikasi macOS mulai dingin ≤ 400 ms; Android ≤ 800 ms. |
| NFR-2 | **Responsivitas mengetik** | Jeda dari ketikan sampai karakter tampil ≤ 16 ms pada slot berisi 50.000 karakter, di kedua platform. |
| NFR-3 | **Kecepatan sinkronisasi** | Perubahan sampai di perangkat lain yang sedang aktif ≤ 3 detik pada jaringan normal. Antrean luring bertahan sampai 30 hari tanpa kehilangan. |
| NFR-4 | **Keutuhan data** | Tidak ada tulisan yang hilang saat aplikasi ditutup paksa. Penulisan lokal dipastikan turun ke disk sebelum antarmuka menyatakan tersimpan. |
| NFR-5 | **Keandalan** | Sesi bebas macet ≥ 99,5%. Kegagalan sinkronisasi mencoba ulang dengan jeda menaik, maksimal 6 percobaan sebelum menyerah dan memberi tahu pengguna. |
| NFR-6 | **Keamanan** | TLS 1.3 untuk seluruh lalu lintas. Token disimpan di Keychain (macOS) dan Android Keystore. RLS aktif di semua tabel tanpa pengecualian. |
| NFR-7 | **Privasi** | Tanpa analitik pihak ketiga, tanpa iklan, tanpa pelatihan model atas isi catatan. Laporan macet bersifat opsional dan mati secara bawaan. |
| NFR-8 | **Aksesibilitas** | Kontras memenuhi WCAG 2.1 AA, diverifikasi pada **kedua** tema — setiap nilai dihitung dua kali sejak mode terang kembali, dan diukur ulang dari tangkapan layar perangkat, bukan hanya dari palet. Satu pengecualian tercatat dan disengaja: angka di dalam pil tab Tugas (4,0–4,2:1), yang teks dan latarnya sehue menurut desain. Seluruh kontrol terbaca VoiceOver dan TalkBack. Navigasi keyboard penuh di macOS. Dynamic Type dihormati di Android. Warna slot tidak pernah menjadi satu-satunya pembeda: ia selalu disertai label teks, dan pita penanda slot membawa pola isian yang berbeda per slot sehingga tetap terbaca tanpa persepsi warna sama sekali. |
| NFR-9 | **Kompatibilitas** | macOS 13 Ventura ke atas, Apple Silicon dan Intel. Android 8.0 (API 26) ke atas. |
| NFR-10 | **Lokalisasi** | Bahasa Indonesia dan Inggris saat peluncuran. Seluruh teks dieksternalisasi sejak M1, tanpa string tertanam di kode. |
| NFR-11 | **Ukuran unduhan** | macOS ≤ 25 MB. Android ≤ 15 MB per varian ABI. |

---

## 12. Keputusan teknologi

| Komponen | Pilihan | Alasan |
|---|---|---|
| **Aplikasi macOS** | Swift + SwiftUI, dengan AppKit untuk ikon menu bar dan jendela panel | Panel menu bar yang mengambang di atas mode layar penuh dan pintasan global hanya bisa dibuat rapi lewat API asli. Ini fitur pembeda utama, jadi tidak boleh dikompromikan demi berbagi kode. |
| **Aplikasi Android** | Kotlin + Jetpack Compose | Widget, ubin Pengaturan Cepat, dan integrasi lembar berbagi semuanya membutuhkan API Android asli. |
| **Basis data lokal** | SQLite — GRDB di macOS, Room di Android | Skema identik di kedua sisi, sehingga logika sinkronisasi bisa ditulis dua kali dari satu spesifikasi tanpa perbedaan perilaku. |
| **Backend** | Supabase — Postgres, Auth, Realtime, RLS | Autentikasi, langganan realtime, dan otorisasi tingkat baris tersedia sekaligus. Bisa dipindahkan sendiri karena intinya Postgres biasa. |
| **Notifikasi** | UNUserNotificationCenter (macOS), WorkManager + AlarmManager (Android) | Pengingat dijadwalkan lokal agar tetap menyala tanpa koneksi dan tanpa mengirim isi tugas ke server push. |
| **Distribusi** | DMG bernotaris + Mac App Store; Google Play | Jalur langsung memungkinkan uji coba tanpa akun toko; jalur toko menjangkau pengguna umum. |
| **CI** | GitHub Actions | Membangun kedua platform, menjalankan uji, dan menandatangani rilis dari satu tempat. |

> **Keputusan: dua basis kode asli, bukan satu lintas platform.** Flutter atau Compose Multiplatform akan memangkas pekerjaan antarmuka, tapi menempatkan risiko justru pada bagian yang membedakan produk ini: panel menu bar macOS. Membangun panel mengambang, pintasan global, dan target jatuh untuk drag lewat lapisan pembungkus berarti melawan kerangka kerja tepat pada fitur andalan. Alternatif tengah — Kotlin Multiplatform untuk model data dan mesin sinkronisasi saja — layak ditinjau ulang di M2, setelah aturan sinkronisasi terbukti stabil dan tidak lagi banyak berubah.

---

## 13. Alur & layar utama

| Alur | Langkah |
|---|---|
| **Tangkap cepat** *(macOS · alur terpenting)* | Tekan `⌥Space` → panel muncul dengan kursor sudah di editor → ketik → tekan `Esc`. Total tanpa satu pun klik tetikus. Ini adalah alur yang diuji paling ketat terhadap NFR-1. |
| **Buka pertama kali** | Aplikasi terbuka langsung ke Slot 1 dengan teks contoh yang menjelaskan konsep lima slot → pengguna mengetik dan teks contoh hilang → ajakan membuat akun baru muncul setelah pemakaian hari kedua, bukan di awal. |
| **Menghubungkan perangkat kedua** | Pengaturan → Akun → masuk → aplikasi mendeteksi data lokal dan data cloud sama-sama ada → tampilkan tiga pilihan (gabungkan / pakai cloud / pakai lokal) dengan pratinjau jumlah baris di masing-masing → sinkronisasi pertama berjalan dengan indikator kemajuan. |
| **Konflik terjadi** | Perangkat lain menimpa slot yang baru diedit → panel kecil muncul di atas editor → pengguna menekan "Lihat versi saya" → daftar revisi terbuka → pulihkan mengembalikan isi lama sebagai revisi baru, bukan dengan menimpa balik. |
| **Mengetik saat luring** | Indikator status berubah jadi luring → pengguna tetap mengedit seperti biasa tanpa peringatan apa pun → saat koneksi pulih, status berubah jadi menyinkronkan lalu tersinkron, tanpa perlu tindakan pengguna. |

---

## 14. Rencana rilis

| Tahap | Cakupan | Durasi | Kriteria selesai |
|---|---|---|---|
| **M1** | **Inti lokal** — FR-1, FR-2, FR-5, FR-6 (P0) | 5 minggu | Kedua aplikasi jalan mandiri tanpa akun. Lima slot dan daftar tugas berfungsi penuh, tersimpan lokal, dan tidak kehilangan data saat ditutup paksa. |
| **M2** | **Akun & sinkronisasi** — FR-3, FR-7.1 | 4 minggu | Perubahan di Mac muncul di Android dalam 3 detik dan sebaliknya. Uji luring 7 hari lolos tanpa kehilangan atau duplikasi baris. Pembeda utama di §5 sudah dapat dibuktikan. |
| **M3** | **Menu bar** — FR-4 | 3 minggu | Alur tangkap cepat memenuhi NFR-1 pada perangkat acuan. Panel dan jendela utama terbukti tidak pernah menampilkan isi yang berbeda. |
| **M4** | **Peluncuran publik** — sisa P1, ekspor, widget, tema | 3 minggu | Seluruh butir P1 selesai. Audit aksesibilitas lolos. Terbit di Google Play dan sebagai DMG bernotaris. |
| **M5** | **Pendalaman** — butir P2, Shortcuts, enkripsi ujung-ke-ujung | 4 minggu | Enkripsi ujung-ke-ujung opsional dengan frasa sandi. Aksi Shortcuts dan skema URL tersedia. Pencarian lintas slot aktif. |

> **Status per 15 September 2026.** Android M1 selesai dan aplikasi macOS lokal sudah berjalan dengan lima editor, tugas berkelompok, penyimpanan GRDB, jendela responsif, serta tema terang/gelap. Menu bar FR-4.1–FR-4.6 dan tambah tugas cepat FR-4.9 sudah diimplementasikan memakai `NSStatusItem`, `NSPanel`, dan pintasan global Carbon: panel dapat diubah ukurannya dan mengingat ukurannya, memakai Store yang sama dengan jendela utama, memfokuskan editor saat dibuka, serta menutup lewat Escape atau kehilangan fokus. FR-4.7/4.8/4.10–FR-4.13 dan pengukuran target 120 ms masih tahap berikutnya. Sinkronisasi M2 belum dimulai.

> **Jalur kritis.** M2 adalah tahap paling berisiko dan paling menentukan. Sebelum M2 selesai, FivePad hanyalah aplikasi catatan lokal biasa tanpa alasan kuat untuk dipilih. Bila jadwal tertekan, potong cakupan M4 — jangan pernah memangkas pengujian M2.

---

## 15. Metrik keberhasilan

| Metrik | Target | Cara ukur |
|---|---|---|
| **Perangkat tertaut** | ≥ 55% | Bagian pengguna aktif yang punya minimal dua perangkat terdaftar. Ini adalah metrik utama, karena membuktikan janji lintas platform benar-benar dipakai. |
| **Retensi hari ke-7** | ≥ 40% | Pengguna yang membuka aplikasi pada hari ke-7 setelah pemasangan. |
| **Pemakaian menu bar** | ≥ 70% | Bagian sesi macOS yang dimulai dari panel menu bar, bukan dari jendela utama. |
| **Tingkat konflik** | ≤ 0,1% | Operasi sinkronisasi yang berakhir dengan satu versi tertimpa. Melewati ambang ini memicu peninjauan ulang keputusan CRDT di §10. |
| **Sesi bebas macet** | ≥ 99,5% | Dari laporan macet opsional dan dasbor toko aplikasi. |
| **Slot terpakai** | rata-rata ≥ 3 | Jumlah slot berisi per pengguna aktif. Angka yang bertahan di bawah 2 berarti batasan lima slot terlalu longgar; angka yang selalu 5 dengan banyak keluhan berarti terlalu ketat. |

---

## 16. Risiko & mitigasi

| Risiko | Dampak | Mitigasi |
|---|---|---|
| **Biaya server tumbuh tanpa pendapatan** | Tinggi | Model harga wajib ditetapkan sebelum M2 dimulai, bukan menjelang peluncuran. Jenjang lokal-saja tetap gratis selamanya sehingga biaya hanya muncul dari pengguna yang menyinkron. |
| **Kehilangan data akibat last-write-wins** | Tinggi | Riwayat versi (FR-3.11) dan pemberitahuan penimpaan (FR-3.12) keduanya berstatus P1, bukan opsional. Tingkat konflik dipantau sebagai metrik di §15. |
| **Bentrok pintasan global** | Sedang | `⌥Space` banyak dipakai peluncur seperti Alfred dan Raycast. Deteksi bentrok saat pemasangan pertama dan tawarkan alternatif, alih-alih membiarkan pengguna menemukan sendiri bahwa pintasannya tidak berfungsi. |
| **Doze mode menunda sinkronisasi Android** | Sedang | Jangan bergantung pada sinkronisasi latar belakang. Selalu tarik saat aplikasi kembali ke latar depan, dan tampilkan status jujur ketimbang menjanjikan kesegaran yang tidak bisa dijamin sistem. |
| **Penolakan tinjauan App Store** | Sedang | Jika ada login pihak ketiga, Masuk dengan Apple wajib disediakan. Aturan pembelian dalam aplikasi diikuti sejak M2, bukan ditambal saat pengajuan. |
| **Tekanan menambah slot keenam** | Sedang | Permintaan ini pasti datang dan harus ditolak — ia menghapus satu-satunya hal yang membedakan produk ini dari aplikasi catatan mana pun. §6 ada untuk menyelesaikan perdebatan itu tanpa mengulangnya tiap kali. |
| **Dua basis kode menyimpang perilakunya** | Rendah | Aturan sinkronisasi diperlakukan sebagai spesifikasi bersama dengan rangkaian uji yang sama dijalankan di kedua platform terhadap satu kumpulan skenario. |

---

## 17. Pertanyaan terbuka

| No | Pertanyaan | Batas | Rekomendasi |
|---|---|---|---|
| Q1 | **Model harga** | Sebelum M2 | Lokal gratis selamanya; sinkronisasi berlangganan murah. Sekali bayar tidak cocok karena biaya server bersifat berulang, sementara pendapatannya tidak. Ini pertanyaan paling mendesak di daftar ini. |
| Q2 | **iOS masuk peta jalan?** | Sebelum M3 | Tunda sampai setelah M4. Menambahkannya sekarang berarti bersaing langsung dengan FiveNotes di kandang mereka, alih-alih melayani celah yang kita pilih. |
| Q3 | **Daftar tugas per slot?** | ~~Sebelum M1~~ · **diputuskan** | Tidak. Daftar tugas tetap satu dan global, tidak terikat slot. Namun sejak FR-2.14 daftar itu boleh dibagi menjadi grup buatan pengguna — pengelompokan di dalam satu daftar, bukan lima daftar terpisah. |
| Q4 | **Enkripsi ujung-ke-ujung: pembeda utama atau fitur lanjutan?** | Sebelum M2 | Fitur lanjutan di M5. Menjadikannya inti akan menutup pemulihan kata sandi dan pratinjau notifikasi — beban yang berat untuk pengguna awal. |
| Q5 | **Label bawaan kelima slot** | Sebelum M1 | Kosongkan labelnya dan tampilkan nomor saja. Label yang disarankan sistem akan mengarahkan pemakaian, padahal keluwesan makna tiap slot justru kekuatannya. |
| Q6 | **Berapa lama uji coba gratis?** | Sebelum M4 | Ikuti pola 30 hari yang lazim di kelas ini, dihitung sejak akun dibuat dan bukan sejak pemasangan. |

---

## 18. Di luar cakupan

### Ditolak secara permanen

- Catatan tanpa batas atau slot keenam.
- Folder, tag, atau hierarki **pada catatan**. Daftar tugas dikecualikan — lihat FR-2.14.
- Kolaborasi, berbagi catatan, dan komentar.
- Iklan dan analitik pihak ketiga.

### Mungkin nanti, bukan sekarang

- Aplikasi iOS dan watchOS (lihat Q2).
- Windows dan Linux.
- Aplikasi web untuk akses darurat.
- Lampiran gambar dan berkas.
- Sinkronisasi lewat penyimpanan sendiri milik pengguna.
- Ekstensi peramban untuk simpan cepat.

---

## 19. Glosarium

| Istilah | Arti dalam dokumen ini |
|---|---|
| **Slot** | Satu dari lima wadah catatan permanen. Berbeda dari "catatan" pada aplikasi lain karena tidak bisa dibuat maupun dihapus — hanya diisi dan dikosongkan. |
| **Panel cepat** | Jendela ringan yang muncul dari ikon menu bar macOS. Bukan jendela aplikasi utama, tapi berbagi data yang sama persis dengannya. |
| **Offline-first** | Pendekatan di mana penyimpanan lokal adalah sumber kebenaran saat pengguna bekerja, dan server hanya menyatukan perangkat. Kebalikannya adalah antarmuka yang menunggu jawaban server sebelum menampilkan hasil. |
| **Last-write-wins** | Aturan konflik di mana perubahan dengan stempel waktu server terbaru menang secara utuh atas baris yang sama. Disingkat LWW. |
| **Baris dirty** | Baris lokal yang sudah berubah tapi belum berhasil dikirim ke server. Antrean baris dirty inilah yang membuat mode luring bekerja. |
| **Penanda sinkronisasi** | Stempel waktu server dari penarikan terakhir yang berhasil. Klien memakainya untuk hanya meminta baris yang lebih baru dari itu. |
| **Soft delete** | Menandai baris terhapus lewat kolom `deleted_at` alih-alih membuangnya. Diperlukan agar perangkat luring tidak menghidupkan kembali data yang sudah dihapus. |
| **RLS** | Row Level Security. Aturan di Postgres yang membatasi baris mana yang boleh dibaca dan ditulis setiap pengguna, ditegakkan oleh basis data sendiri, bukan oleh kode aplikasi. |

---

*Dokumen ini ditinjau ulang di akhir setiap tahap M1–M5.*
