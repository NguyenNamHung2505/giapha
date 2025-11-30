import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject } from 'rxjs';

export interface Language {
  code: string;
  name: string;
  flag: string;
}

@Injectable({
  providedIn: 'root'
})
export class LanguageService {
  private readonly LANG_KEY = 'selected_language';

  readonly languages: Language[] = [
    { code: 'vi', name: 'Tiếng Việt', flag: '🇻🇳' },
    { code: 'en', name: 'English', flag: '🇬🇧' }
  ];

  private currentLangSubject = new BehaviorSubject<string>('vi');
  currentLang$ = this.currentLangSubject.asObservable();

  constructor(private translate: TranslateService) {
    this.initLanguage();
  }

  private initLanguage(): void {
    // Set available languages
    this.translate.addLangs(['vi', 'en']);

    // Get saved language or use default
    const savedLang = localStorage.getItem(this.LANG_KEY);
    const browserLang = this.translate.getBrowserLang();

    let defaultLang = 'vi'; // Default to Vietnamese

    if (savedLang && this.languages.some(l => l.code === savedLang)) {
      defaultLang = savedLang;
    } else if (browserLang && this.languages.some(l => l.code === browserLang)) {
      // Only use browser language if it's one of our supported languages
      // but still prefer Vietnamese as default
      defaultLang = 'vi';
    }

    this.translate.setDefaultLang('vi');
    this.setLanguage(defaultLang);
  }

  setLanguage(langCode: string): void {
    if (this.languages.some(l => l.code === langCode)) {
      this.translate.use(langCode);
      localStorage.setItem(this.LANG_KEY, langCode);
      this.currentLangSubject.next(langCode);
    }
  }

  getCurrentLanguage(): string {
    return this.currentLangSubject.value;
  }

  getLanguageByCode(code: string): Language | undefined {
    return this.languages.find(l => l.code === code);
  }
}
