import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LocaleService {
  private readonly LANG_KEY = 'selected_language';

  getLocale(): string {
    const savedLang = localStorage.getItem(this.LANG_KEY);
    return savedLang === 'en' ? 'en-US' : 'vi';
  }
}
